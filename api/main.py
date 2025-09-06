import asyncio
from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager as MediaManager
import json
import websockets
import socket


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

async def toggle_media():
    try:
        sessions = await MediaManager.request_async()
        current_session = sessions.get_current_session()
        
        if not current_session:
            return {"success": False, "message": "No active media session found"}
        
        playback_info = current_session.get_playback_info()
        is_playing = playback_info.playback_status == 4  # 4 = Playing
        
        if is_playing:
            success = await current_session.try_pause_async()
            if success:
                return {"success": True, "message": "Media paused successfully"}
            else:
                return {"success": False, "message": "Failed to pause media"}
        else:
            success = await current_session.try_play_async()
            if success:
                return {"success": True, "message": "Media resumed successfully"}
            else:
                return {"success": False, "message": "Failed to resume media"}
    except Exception as e:
        print(f"Error toggling media: {e}")
        return {"success": False, "message": f"Error: {str(e)}"}


async def handle_client(websocket):
    try:
        async for message in websocket:
            match message:
                    case "get_media":
                        media_info = await get_media_info()
                        await websocket.send(json.dumps(media_info))
                    case "toggle_playback":
                        result = await toggle_media()
                        await websocket.send(json.dumps(result))        
                    case _:  # Default case
                        await websocket.send(json.dumps({"error": "Unknown command"}))
    except websockets.exceptions.ConnectionClosed:
        pass

def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()

async def start_websocket_server(host=get_ip(), port=8765):
    server = await websockets.serve(handle_client, host, port)
    print(f"WebSocket server started at ws://{host}:{port}")
    await server.wait_closed()

if __name__ == "__main__":
    import sys
    

    print("Starting WebSocket server...")
    asyncio.run(start_websocket_server())
