# File Tree: EC2 deployment structure

**Root Path:** `ubuntu@ip-172-31-37-58:/opt/log-monitor$`

```
├── elasticsearch
│   ├── config
│   │   └── init-es.sh
│   ├── ilm-policies
│   │   └── ogs-ilm.json
│   └── index-templates
│       └── log-template.json
│
├── nginx
│   └── default.conf
│
├── logstash
│   └── logstash.conf
│
└── docker-compose.yml
```

---