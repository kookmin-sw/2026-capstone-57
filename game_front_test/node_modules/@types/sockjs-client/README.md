# Installation
> `npm install --save @types/sockjs-client`

# Summary
This package contains type definitions for sockjs-client (https://github.com/sockjs/sockjs-client).

# Details
Files were exported from https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/sockjs-client.
## [index.d.ts](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/sockjs-client/index.d.ts)
````ts
export = SockJS;
export as namespace SockJS;

declare const SockJS: {
    new(url: string, _reserved?: any, options?: SockJS.Options): WebSocket;
    (url: string, _reserved?: any, options?: SockJS.Options): WebSocket;
    prototype: WebSocket;
    CONNECTING: SockJS.CONNECTING;
    OPEN: SockJS.OPEN;
    CLOSING: SockJS.CLOSING;
    CLOSED: SockJS.CLOSED;
};

declare namespace SockJS {
    type CONNECTING = 0;
    type OPEN = 1;
    type CLOSING = 2;
    type CLOSED = 3;

    type State = CONNECTING | OPEN | CLOSING | CLOSED;

    interface BaseEvent extends Event {
        type: string;
    }

    type OpenEvent = BaseEvent;

    interface CloseEvent extends BaseEvent {
        code: number;
        reason: string;
        wasClean: boolean;
    }

    interface MessageEvent extends BaseEvent {
        data: string;
    }

    type SessionGenerator = () => string;

    interface Options {
        server?: string | undefined;
        sessionId?: number | SessionGenerator | undefined;
        transports?: string | string[] | undefined;
        timeout?: number | undefined;
    }
}

````

### Additional Details
 * Last updated: Tue, 07 Nov 2023 15:11:36 GMT
 * Dependencies: none

# Credits
These definitions were written by [Emil Ivanov](https://github.com/vladev), [Alexander Rusakov](https://github.com/arusakov), [BendingBender](https://github.com/BendingBender), [Soner Köksal](https://github.com/renjfk), and [Alexander Putilov](https://github.com/PutilovAI).
