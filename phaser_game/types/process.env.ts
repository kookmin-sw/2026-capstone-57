// define the Node "process" object. If it is defined (@types/node) by another module you installed,
// then you can delete this file

declare const process: {
    env: {
        NODE_ENV: string
    }
}

// Vite environment variable types
interface ImportMetaEnv {
    readonly VITE_WS_URL: string;
    readonly VITE_API_BASE_URL: string;
    readonly VITE_LOCAL_MODE: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}