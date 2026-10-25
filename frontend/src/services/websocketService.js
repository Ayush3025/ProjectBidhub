
import { Client } from "@stomp/stompjs";

const WS_URL = "ws://localhost:8080/ws";

export function connectToAuction(
    auctionId,
    onBidUpdate,
    onConnectionChange
) {
    const client = new Client({
        brokerURL: WS_URL,
        reconnectDelay: 5000,

        onConnect: () => {
            console.log("WebSocket connected");

            // Notify the page that the connection is active.
            onConnectionChange?.(true);

            // Subscribe to updates for this specific auction.
            client.subscribe(
                `/topic/auctions/${auctionId}`,
                (message) => {
                    try {
                        const bidUpdate = JSON.parse(message.body);
                        onBidUpdate(bidUpdate);
                    } catch (error) {
                        console.error(
                            "Failed to parse bid update:",
                            error
                        );
                    }
                }
            );
        },

        onStompError: (frame) => {
            console.error(
                "STOMP error:",
                frame.headers["message"]
            );
        },

        onWebSocketError: (error) => {
            console.error("WebSocket error:", error);
        },

        onWebSocketClose: () => {
            console.log("WebSocket disconnected");
            onConnectionChange?.(false);
        },
    });

    client.activate();

    // Disconnect when the component using this service unmounts.
    return () => {
        onConnectionChange?.(false);
        client.deactivate();
    };
}
