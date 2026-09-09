# VPS Deployment Runbook & Security Checklist

This document is an operational, copy-pasteable checklist for deploying the **Witchfire Randomizer** application onto a Linux VPS.

---

## Architecture Overview

```
Internet (443/HTTPS, 80/HTTP)
           │
           ▼
     [Host Firewall (UFW)]
           │
           ▼
     [Caddy Reverse Proxy] (Automatic TLS, Security Headers, Gzip/Zstd)
           │  127.0.0.1:9090
           ▼
[Spring Boot Application] (witchfire.service, sandbox-confined, user: witchfire)
```

---

## Checklist

### Phase 1: Local Build & Artifact Preparation

- [ ] **1. Run tests and package the production JAR**
  ```bash
  ./mvnw clean package
  ```
  Artifact produced: `target/witchfirerandomizer-0.0.1-SNAPSHOT.jar`

- [ ] **2. Copy files to your VPS**
  ```bash
  export VPS_IP="your-vps-ip"
  export VPS_USER="your-ssh-user"

  # Copy JAR binary
  scp target/witchfirerandomizer-0.0.1-SNAPSHOT.jar ${VPS_USER}@${VPS_IP}:/tmp/witchfirerandomizer.jar

  # Copy deployment templates
  scp deploy/witchfire.service ${VPS_USER}@${VPS_IP}:/tmp/witchfire.service
  scp deploy/Caddyfile ${VPS_USER}@${VPS_IP}:/tmp/Caddyfile
  ```

---

### Phase 2: VPS System Prerequisites & OS Hardening

SSH into your VPS (`ssh ${VPS_USER}@${VPS_IP}`) and run:

- [ ] **1. Update operating system packages**
  ```bash
  sudo apt update && sudo apt upgrade -y
  ```

- [ ] **2. Install Java 25 Runtime (Eclipse Temurin or OpenJDK)**
  ```bash
  # Example: Eclipse Temurin 25 (Ubuntu/Debian)
  sudo apt install -y wget apt-transport-https gnupg
  wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
  sudo apt update
  sudo apt install -y temurin-25-jre

  # Verify Java version (must report 25)
  java -version
  ```

- [ ] **3. Install Caddy Web Server**
  ```bash
  sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
  sudo apt update
  sudo apt install -y caddy
  ```

- [ ] **4. Configure Host Firewall (UFW)**
  ```bash
  # Set default policies
  sudo ufw default deny incoming
  sudo ufw default allow outgoing

  # Allow essential services only
  sudo ufw allow 22/tcp comment 'SSH'
  sudo ufw allow 80/tcp comment 'HTTP (Caddy ACME challenge)'
  sudo ufw allow 443/tcp comment 'HTTPS'

  # Enable firewall
  sudo ufw enable
  sudo ufw status verbose
  ```
  > [!IMPORTANT]
  > Verify port `9090` is **NOT** allowed in UFW. External traffic to port 9090 must be dropped.

---

### Phase 3: Dedicated User & Directory Setup

- [ ] **1. Create an unprivileged system user**
  ```bash
  sudo useradd -r -s /bin/false witchfire
  ```

- [ ] **2. Create application directories with hardened permissions**
  ```bash
  sudo mkdir -p /opt/witchfirerandomizer/logs
  sudo mkdir -p /var/log/caddy

  # Install JAR file (owned by root, read-only to witchfire)
  sudo mv /tmp/witchfirerandomizer.jar /opt/witchfirerandomizer/witchfirerandomizer.jar
  sudo chown root:root /opt/witchfirerandomizer/witchfirerandomizer.jar
  sudo chmod 644 /opt/witchfirerandomizer/witchfirerandomizer.jar

  # Logs directory (owned and writable only by witchfire)
  sudo chown -R witchfire:witchfire /opt/witchfirerandomizer/logs
  sudo chmod 750 /opt/witchfirerandomizer/logs

  # Caddy log directory
  sudo chown -R caddy:caddy /var/log/caddy
  sudo chmod 750 /var/log/caddy
  ```

---

### Phase 4: Systemd Service Installation

- [ ] **1. Install the systemd service file**
  ```bash
  sudo mv /tmp/witchfire.service /etc/systemd/system/witchfire.service
  sudo chown root:root /etc/systemd/system/witchfire.service
  sudo chmod 644 /etc/systemd/system/witchfire.service
  ```

- [ ] **2. Reload systemd daemon and start service**
  ```bash
  sudo systemctl daemon-reload
  sudo systemctl enable --now witchfire.service
  ```

- [ ] **3. Verify service status**
  ```bash
  sudo systemctl status witchfire.service
  ```
  Expected output: `Active: active (running)`.

---

### Phase 5: Caddy Reverse Proxy & TLS Setup

- [ ] **1. Update domain name in Caddyfile**
  Edit `/tmp/Caddyfile` and replace `yourdomain.com` with your actual domain or VPS hostname:
  ```bash
  sudo nano /tmp/Caddyfile
  ```

- [ ] **2. Deploy Caddy configuration**
  ```bash
  sudo mv /tmp/Caddyfile /etc/caddy/Caddyfile
  sudo chown root:root /etc/caddy/Caddyfile
  sudo chmod 644 /etc/caddy/Caddyfile
  ```

- [ ] **3. Validate configuration and reload Caddy**
  ```bash
  sudo caddy validate --config /etc/caddy/Caddyfile
  sudo systemctl reload caddy
  ```

---

### Phase 6: Post-Deployment Verification

Perform these verification checks from your local machine or terminal:

- [ ] **1. Test local backend response on VPS**
  ```bash
  curl -I http://127.0.0.1:9090
  ```
  Expected: HTTP 200 OK with `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY`.

- [ ] **2. Test external HTTPS endpoint**
  ```bash
  curl -I https://yourdomain.com
  ```
  Expected:
  - `HTTP/2 200` (or `HTTP/3 200`)
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - `Content-Security-Policy: default-src 'self' ...`
  - `X-Frame-Options: DENY`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - No `Server: ...` banner (suppressed by Caddy)

- [ ] **3. Confirm port 9090 is NOT accessible externally**
  ```bash
  # Run from your local laptop (not inside the VPS):
  curl --connect-timeout 5 http://YOUR_VPS_IP:9090
  ```
  Expected: Connection timed out or connection refused (UFW blocks incoming traffic).

- [ ] **4. Check live service logs**
  ```bash
  # Application logs (Spring Boot)
  sudo journalctl -u witchfire.service -n 50 --no-pager

  # Caddy access logs
  sudo tail -n 20 /var/log/caddy/witchfire_access.log
  ```

---

## Updating the Application (Future Releases)

When releasing an update, follow this streamlined procedure:

1. **Build new JAR locally:**
   ```bash
   ./mvnw clean package
   ```
2. **Transfer new JAR to VPS:**
   ```bash
   scp target/witchfirerandomizer-0.0.1-SNAPSHOT.jar ${VPS_USER}@${VPS_IP}:/tmp/witchfirerandomizer.jar
   ```
3. **Replace binary and restart service on VPS:**
   ```bash
   sudo mv /tmp/witchfirerandomizer.jar /opt/witchfirerandomizer/witchfirerandomizer.jar
   sudo chown root:root /opt/witchfirerandomizer/witchfirerandomizer.jar
   sudo chmod 644 /opt/witchfirerandomizer/witchfirerandomizer.jar
   sudo systemctl restart witchfire.service
   ```
4. **Confirm healthy status:**
   ```bash
   sudo systemctl status witchfire.service
   ```
