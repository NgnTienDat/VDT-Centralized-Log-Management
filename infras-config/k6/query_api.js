import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    vus: 20,
    duration: '3m',

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

// const BASE_URL = 'http://localhost:8082/api/v1/logs';
const BASE_URL = 'http://47.128.219.78/api/v1/logs';

export default function () {
    const params = {
        size: 20,
        environment: 'staging',
        appName: 'logs-app',
        serviceName: 'logs-service',
        logLevel: 'ERROR',
        q: 'Payment processing failed'
    };

    const qs = Object.entries(params)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&');

    const url = `${BASE_URL}?${qs}`;

    http.get(url);
    sleep(0.1);
}