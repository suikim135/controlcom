using System.Security.Cryptography;
using System.Text.Json;

namespace ControlCom.Agent.Services;

public sealed class TokenService
{
    private const string TokenFileName = "token.json";
    private const string PairingCodeFileName = "pairing-code.txt";
    private const string PairingStateFileName = "pairing-state.json";

    private readonly string _dataDirectory;
    private readonly int _maxPairingAttempts;
    private readonly int _pairingLockoutMinutes;
    private readonly int _pairingCodeTtlMinutes;
    private readonly object _lock = new();
    private string? _token;
    private string? _pairingCode;
    private DateTimeOffset? _pairingCodeCreatedAt;
    private int _failedPairingAttempts;
    private DateTimeOffset? _pairingLockedUntil;

    public TokenService(IConfiguration configuration)
    {
        _maxPairingAttempts = configuration.GetValue("Agent:MaxPairingAttempts", 5);
        _pairingLockoutMinutes = configuration.GetValue("Agent:PairingLockoutMinutes", 15);
        _pairingCodeTtlMinutes = configuration.GetValue("Agent:PairingCodeTtlMinutes", 30);

        _dataDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "ControlCom",
            "Agent");
        Directory.CreateDirectory(_dataDirectory);
        Load();
    }

    public bool HasToken
    {
        get
        {
            lock (_lock)
            {
                return !string.IsNullOrWhiteSpace(_token);
            }
        }
    }

    public string GetPairingCode()
    {
        lock (_lock)
        {
            if (HasToken)
            {
                return string.Empty;
            }

            if (!string.IsNullOrWhiteSpace(_pairingCode) && !IsPairingCodeExpired())
            {
                return _pairingCode;
            }

            _pairingCode = RandomNumberGenerator.GetInt32(100000, 999999).ToString();
            _pairingCodeCreatedAt = DateTimeOffset.UtcNow;
            SavePairingState();
            File.WriteAllText(GetPairingCodePath(), _pairingCode);
            return _pairingCode;
        }
    }

    public bool TryPair(string code, out string token)
    {
        lock (_lock)
        {
            token = string.Empty;
            if (IsPairingLockedOut())
            {
                return false;
            }

            if (HasToken)
            {
                return false;
            }

            if (IsPairingCodeExpired())
            {
                _pairingCode = null;
                _pairingCodeCreatedAt = null;
                return false;
            }

            if (!string.Equals(code.Trim(), _pairingCode, StringComparison.Ordinal))
            {
                _failedPairingAttempts++;
                if (_failedPairingAttempts >= _maxPairingAttempts)
                {
                    _pairingLockedUntil = DateTimeOffset.UtcNow.AddMinutes(_pairingLockoutMinutes);
                }

                SavePairingState();
                return false;
            }

            _token = Guid.NewGuid().ToString("N");
            File.WriteAllText(GetTokenPath(), JsonSerializer.Serialize(new TokenData { Token = _token }));
            File.Delete(GetPairingCodePath());
            _pairingCode = null;
            _pairingCodeCreatedAt = null;
            _failedPairingAttempts = 0;
            _pairingLockedUntil = null;
            SavePairingState();
            token = _token;
            return true;
        }
    }

    public bool IsPairingLockedOut()
    {
        lock (_lock)
        {
            if (_pairingLockedUntil is null)
            {
                return false;
            }

            if (_pairingLockedUntil <= DateTimeOffset.UtcNow)
            {
                _pairingLockedUntil = null;
                _failedPairingAttempts = 0;
                SavePairingState();
                return false;
            }

            return true;
        }
    }

    public int GetPairingLockoutSecondsRemaining()
    {
        lock (_lock)
        {
            if (_pairingLockedUntil is null)
            {
                return 0;
            }

            var remaining = (int)Math.Ceiling((_pairingLockedUntil.Value - DateTimeOffset.UtcNow).TotalSeconds);
            return Math.Max(remaining, 0);
        }
    }

    public bool IsValid(string? token)
    {
        lock (_lock)
        {
            return !string.IsNullOrWhiteSpace(_token)
                && !string.IsNullOrWhiteSpace(token)
                && string.Equals(_token, token, StringComparison.Ordinal);
        }
    }

    public void ResetPairing()
    {
        lock (_lock)
        {
            _token = null;
            _pairingCode = null;
            _pairingCodeCreatedAt = null;
            _failedPairingAttempts = 0;
            _pairingLockedUntil = null;

            if (File.Exists(GetTokenPath()))
            {
                File.Delete(GetTokenPath());
            }

            if (File.Exists(GetPairingCodePath()))
            {
                File.Delete(GetPairingCodePath());
            }

            SavePairingState();
        }
    }

    private bool IsPairingCodeExpired()
    {
        if (_pairingCodeCreatedAt is null)
        {
            return false;
        }

        return _pairingCodeCreatedAt.Value.AddMinutes(_pairingCodeTtlMinutes) < DateTimeOffset.UtcNow;
    }

    private void Load()
    {
        var tokenPath = GetTokenPath();
        if (File.Exists(tokenPath))
        {
            var json = File.ReadAllText(tokenPath);
            var data = JsonSerializer.Deserialize<TokenData>(json);
            _token = data?.Token;
        }

        var pairingCodePath = GetPairingCodePath();
        if (File.Exists(pairingCodePath))
        {
            _pairingCode = File.ReadAllText(pairingCodePath).Trim();
        }

        var pairingStatePath = GetPairingStatePath();
        if (File.Exists(pairingStatePath))
        {
            var state = JsonSerializer.Deserialize<PairingStateData>(File.ReadAllText(pairingStatePath));
            if (state is not null)
            {
                _pairingCodeCreatedAt = state.PairingCodeCreatedAt;
                _failedPairingAttempts = state.FailedPairingAttempts;
                _pairingLockedUntil = state.PairingLockedUntil;
            }
        }

        if (IsPairingCodeExpired())
        {
            _pairingCode = null;
            _pairingCodeCreatedAt = null;
            if (File.Exists(GetPairingCodePath()))
            {
                File.Delete(GetPairingCodePath());
            }
        }
    }

    private void SavePairingState()
    {
        var state = new PairingStateData
        {
            PairingCodeCreatedAt = _pairingCodeCreatedAt,
            FailedPairingAttempts = _failedPairingAttempts,
            PairingLockedUntil = _pairingLockedUntil
        };
        File.WriteAllText(GetPairingStatePath(), JsonSerializer.Serialize(state));
    }

    private string GetTokenPath() => Path.Combine(_dataDirectory, TokenFileName);
    private string GetPairingCodePath() => Path.Combine(_dataDirectory, PairingCodeFileName);
    private string GetPairingStatePath() => Path.Combine(_dataDirectory, PairingStateFileName);

    private sealed class TokenData
    {
        public string Token { get; set; } = string.Empty;
    }

    private sealed class PairingStateData
    {
        public DateTimeOffset? PairingCodeCreatedAt { get; set; }
        public int FailedPairingAttempts { get; set; }
        public DateTimeOffset? PairingLockedUntil { get; set; }
    }
}
