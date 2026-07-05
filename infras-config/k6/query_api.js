import http from 'k6/http';

export const options = {
    vus: 20,
    duration: '1m',

    thresholds: {
        http_req_duration: [
            'avg<300',
            'p(90)<450',
            'p(95)<500',
            'max<2000'
        ],
        http_req_failed: [
            'rate<0.01'
        ]
    }
};

export default function () {
    http.get(
        'http://47.128.219.78/api/v1/logs'
        // 'http://47.128.219.78/api/v1/logs?size=50&environment=staging&appName=logs-app&serviceName=logs-service&logLevel=INFO&q=User'
    );
}