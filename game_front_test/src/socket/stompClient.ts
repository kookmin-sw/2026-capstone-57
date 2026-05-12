import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface StompClientConfig {
  wsEndpoint: string;
  token: string;
  onConnect?: (frame: IFrame) => void;
  onDisconnect?: (frame: IFrame) => void;
  onStompError?: (frame: IFrame) => void;
  onWebSocketClose?: (event: CloseEvent) => void;
  onWebSocketError?: (event: Event) => void;
  debug?: (msg: string) => void;
}

/**
 * @stomp/stompjs 래퍼.
 * SockJS webSocketFactory를 사용하여 /ws/game 엔드포인트에 연결하고,
 * STOMP CONNECT 프레임의 connectHeaders에 Authorization: Bearer {token}을 포함합니다.
 */
export function createStompClient(config: StompClientConfig): Client {
  const client = new Client({
    // SockJS를 통한 WebSocket 연결 (브라우저 WebSocket API는 커스텀 HTTP 헤더 미지원)
    webSocketFactory: () => new SockJS(config.wsEndpoint),

    // STOMP CONNECT 프레임에 JWT 포함 (HTTP 헤더가 아님)
    connectHeaders: {
      Authorization: `Bearer ${config.token}`,
    },

    // 재연결은 NetworkSystem에서 관리하므로 라이브러리 자동 재연결 비활성화
    reconnectDelay: 0,

    // Heartbeat 설정 (서버와 연결 유지 확인)
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    // 디버그 로깅 (개발 시 활성화 가능)
    debug: config.debug ?? (() => {}),
  });

  if (config.onConnect) {
    client.onConnect = config.onConnect;
  }

  if (config.onDisconnect) {
    client.onDisconnect = config.onDisconnect;
  }

  if (config.onStompError) {
    client.onStompError = config.onStompError;
  }

  if (config.onWebSocketClose) {
    client.onWebSocketClose = config.onWebSocketClose;
  }

  if (config.onWebSocketError) {
    client.onWebSocketError = config.onWebSocketError;
  }

  return client;
}

export type { Client, IFrame, IMessage, StompSubscription };
