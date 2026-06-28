# Add inbound firewall rule for ControlCom Agent (run as Administrator)
param(
    [int]$Port = 7847
)

$ruleName = "ControlCom Agent TCP $Port"

$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Firewall rule already exists: $ruleName"
    exit 0
}

New-NetFirewallRule `
    -DisplayName $ruleName `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort $Port `
    -Profile Private

Write-Host "Firewall rule created for TCP port $Port (Private profile)."
