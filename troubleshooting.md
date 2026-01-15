# Troubleshooting

## Application fails to start when connected to VPN

If you are using a VPN (like Cloudflare Zero Trust), the application might fail to start due to connection errors with the Postgres database.

**Symptom:** Cannot connect to the local Postgres container when the VPN is on. The connection works fine when the VPN is off.

**Cause:** Docker Compose creates a dedicated network (e.g., `172.18.x.x`) which is routed through the VPN tunnel instead of staying local.

**Solution:**
1. Add the network of the Postgres container to the list of excluded networks in VPN settings. If you are using a company-managed Cloudflare Zero Trust, ask IT to add it for you (write on the #it-cloudflare-zero-trust channel on Slack).
2. If it still doesn't work, check which networks are excluded (for Cloudflare you can use `warp-cli settings` command). If it is only the default Docker network (`172.17.0.0/16`), add the following line to your `docker-compose.yml`:

```yaml
services:
  postgres:
    # ...
    network_mode: bridge
    # ...
 ```
