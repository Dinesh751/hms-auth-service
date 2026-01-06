# HMS Auth Service - Prometheus Monitoring Queries

## 🔍 Essential Prometheus Queries for Your HMS Auth Service

### Authentication Monitoring
```prometheus
# Login success rate (percentage)
(rate(auth_login_success_total[5m]) / (rate(auth_login_success_total[5m]) + rate(auth_login_failure_total[5m]))) * 100

# Failed login attempts per minute (security alert)
rate(auth_login_failure_total[1m]) * 60

# JWT token generation rate
rate(auth_jwt_generated_total[5m])

# Authentication response time (95th percentile)
histogram_quantile(0.95, auth_authentication_duration_bucket)
```

### Performance Monitoring
```prometheus
# HTTP request rate
rate(http_server_requests_seconds_count[5m])

# Error rate (4xx and 5xx responses)
rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])

# Database connection pool usage
hikaricp_connections_active / hikaricp_connections_max * 100

# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Business Metrics
```prometheus
# Active user count
auth_users_active

# User registration rate
rate(auth_registration_total[1h])

# User growth trend
increase(auth_registration_total[24h])
```

## 🚨 Alerting Rules

### Critical Alerts
```yaml
groups:
  - name: hms_auth_critical
    rules:
    - alert: HighFailedLoginRate
      expr: rate(auth_login_failure_total[5m]) > 0.1
      for: 2m
      annotations:
        summary: "High failed login attempts detected"
        
    - alert: AuthServiceDown
      expr: up == 0
      for: 1m
      annotations:
        summary: "HMS Auth Service is down"
        
    - alert: DatabaseConnectionHigh
      expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
      for: 5m
      annotations:
        summary: "Database connection pool usage > 80%"
```

## 📈 Grafana Dashboard Panels

### 1. Authentication Overview
- Login success rate (gauge)
- Failed login attempts (counter)
- JWT tokens generated (counter)
- Authentication response time (graph)

### 2. Performance Metrics
- HTTP request rate (graph)
- Error rate percentage (gauge)
- Response time distribution (histogram)
- Database connections (gauge)

### 3. Business KPIs
- Active users (stat)
- New registrations (counter)
- User growth trend (graph)
- Role distribution (pie chart)

## 🎯 What This Enables

### Real-time Monitoring
✅ **Instant visibility** into authentication failures
✅ **Performance tracking** of login response times  
✅ **Security monitoring** for suspicious patterns
✅ **Capacity planning** with user growth trends

### Automated Alerting
✅ **PagerDuty/Slack alerts** for critical failures
✅ **Email notifications** for performance degradation
✅ **Auto-scaling triggers** based on load metrics
✅ **Security alerts** for brute force attempts

### Historical Analysis
✅ **Trend analysis** of user behavior
✅ **Performance baselines** for capacity planning
✅ **Incident investigation** with detailed metrics
✅ **SLA reporting** for business stakeholders

## 🌍 Cloud Integration Examples

### AWS CloudWatch
```yaml
# Export to CloudWatch
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: HMS/AuthService
        step: PT1M
```

### Azure Monitor
```yaml
# Export to Azure Monitor
management:
  metrics:
    export:
      azure-monitor:
        enabled: true
        instrumentation-key: ${AZURE_MONITOR_KEY}
```

### Google Cloud Monitoring
```yaml
# Export to Stackdriver
management:
  metrics:
    export:
      stackdriver:
        enabled: true
        project-id: ${GCP_PROJECT_ID}
```

## 💡 Key Takeaway

**Your custom metrics code + Prometheus = Production-grade observability!**

What you've built is exactly what companies like Netflix, Uber, and LinkedIn use for monitoring their authentication services. The combination of:

1. **Custom Application Metrics** (your code) ✅
2. **Prometheus Collection** (time-series data) ✅  
3. **Grafana Visualization** (dashboards) 📊
4. **AlertManager** (automated alerts) 🚨

...creates a complete monitoring stack that's used by the biggest tech companies in the world!
