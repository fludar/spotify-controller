import asyncio
from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager as MediaManager
import json
import websockets


async def get_media_info():
    try:
        
        sessions = await MediaManager.request_async()
        current_session = sessions.get_current_session()
        
        if not current_session:
            return {"is_playing": False}
        
        playback_info = current_session.get_playback_info()
        is_playing = playback_info.playback_status == 4  # 4 = Playing
        
        if not is_playing:
            return {"is_playing": False}
            
        media_props = await current_session.try_get_media_properties_async()
        
        return {
            "is_playing": True,
            "title": media_props.title,
            "artist": media_props.artist,
            "app": current_session.source_app_user_model_id
        }
    except Exception as e:
        print(f"Error: {e}")
        return {"is_playing": False, "error": str(e)}

def is_media_playing():
    return asyncio.run(get_media_info())

async def handle_client(websocket):
    try:
            async for message in websocket:
                if message == "get_media":
                    media_info = await get_media_info()
                    await websocket.send(json.dumps(media_info))
                else:
                    await websocket.send(json.dumps({"error": "Unknown command"}))
    except websockets.exceptions.ConnectionClosed:
        pass

async def start_websocket_server(host="localhost", port=8765):
    """Start a WebSocket server that sends current media playing info when requested."""
    server = await websockets.serve(handle_client, host, port)
    print(f"WebSocket server started at ws://{host}:{port}")
    await server.wait_closed()

if __name__ == "__main__":
    import sys
    
    # Run as WebSocket server
    print("Starting WebSocket server...")
    asyncio.run(start_websocket_server())
