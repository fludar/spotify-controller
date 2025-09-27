import asyncio
from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager as MediaManager
from winsdk.windows.storage.streams import DataReader, InputStreamOptions
import json
import websockets
import socket
import base64
import pyaudiowpatch as pyaudio

async def get_audio():
    try:
        def query_audio_devices():
            devices = []
            p = pyaudio.PyAudio()
            try:
                for i in range(p.get_device_count()):
                    device_info = p.get_device_info_by_index(i)
                    if device_info.get('maxOutputChannels', 0) > 0:
                        devices.append(device_info)
            finally:
                p.terminate()
            return devices
        
        loop = asyncio.get_event_loop()
        devices = await loop.run_in_executor(None, query_audio_devices)

        output_devices = []
        seen_names = set()  
        
        for device in devices:
            name = device['name'].strip()
            if (name.endswith("()") or 
                name.endswith("(Speaker)") or
                (name.startswith("SPDIF Out") and "S/PDIF Out" in name) or
                name.startswith("Output")):
                continue
                
            if name not in seen_names and name.endswith(')'):
                seen_names.add(name)
                output_devices.append(name)
        
        return output_devices
    except Exception as e:
        print(f"Error in get_audio: {e}")
        return []

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
        
        timeline_props = current_session.get_timeline_properties()
        position_in_seconds = int(timeline_props.position.total_seconds())
        duration_in_seconds = int(timeline_props.end_time.total_seconds())

        position_formatted = f"{position_in_seconds // 60}:{position_in_seconds % 60:02d}"
        duration_formatted = f"{duration_in_seconds // 60}:{duration_in_seconds % 60:02d}"
        
        return {
            "is_playing": True,
            "title": media_props.title,
            "artist": media_props.artist,
            "app": current_session.source_app_user_model_id,
            "position": position_in_seconds,
            "duration": duration_in_seconds,
            "position_formatted": position_formatted,
            "duration_formatted": duration_formatted,
            "progress_percent": round((position_in_seconds / duration_in_seconds * 100) if duration_in_seconds > 0 else 0, 2)
        }
    except Exception as e:
        print(f"Error: {e}")
        return {"is_playing": False, "error": str(e)}

def is_media_playing():
    return asyncio.run(get_media_info())

async def get_thumbnail():
    try:
        sessions = await MediaManager.request_async()
        current_session = sessions.get_current_session()
        
        if not current_session:
            return None
        
        media_props = await current_session.try_get_media_properties_async()
        thumbnail = await media_props.thumbnail.open_read_async()
        data_reader = DataReader(thumbnail)
        data_reader.input_stream_options = InputStreamOptions.READ_AHEAD

        buffer_size = thumbnail.size
        buffer = await data_reader.load_async(int(buffer_size))
        thumbnail_bytes = bytearray(buffer_size)
        data_reader.read_bytes(thumbnail_bytes)

        data_reader.close()
        thumbnail.close()
        
        thumbnail_base64 = base64.b64encode(thumbnail_bytes).decode('utf-8')
        
        return thumbnail_base64
    except Exception as e:
        print(f"Error fetching thumbnail: {e}")
        return None

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

async def next_track():
    try:
        sessions = await MediaManager.request_async()
        current_session = sessions.get_current_session()
        
        if not current_session:
            return {"success": False, "message": "No active media session found"}
        
        success = await current_session.try_skip_next_async()
        if success:
            return {"success": True, "message": "Skipped to next track successfully"}
        else:
            return {"success": False, "message": "Failed to skip to next track"}
    except Exception as e:
        print(f"Error skipping to next track: {e}")
        return {"success": False, "message": f"Error: {str(e)}"}
    
async def previous_track():
    try:
        sessions = await MediaManager.request_async()
        current_session = sessions.get_current_session()
        
        if not current_session:
            return {"success": False, "message": "No active media session found"}
        
        success = await current_session.try_skip_previous_async()
        if success:
            return {"success": True, "message": "Skipped to previous track successfully"}
        else:
            return {"success": False, "message": "Failed to skip to previous track"}
    except Exception as e:
        print(f"Error skipping to previous track: {e}")
        return {"success": False, "message": f"Error: {str(e)}"}

async def handle_client(websocket):
    try:
        async for message in websocket:
            match message:
                    case "get_media":
                        media_info = await get_media_info()
                        await websocket.send(json.dumps(media_info))
                    case "get_thumbnail":
                        thumbnail = await get_thumbnail()
                        if thumbnail is None:
                            await websocket.send("")
                        else:
                            await websocket.send(thumbnail)
                    case "get_audio_devices":
                        audio_devices = await get_audio()
                        await websocket.send(json.dumps(audio_devices))
                    case "toggle_playback":
                        result = await toggle_media()
                        await websocket.send(json.dumps(result))       
                    case "next":
                        next = await next_track()
                        await websocket.send(json.dumps(next))
                    case "prev":
                        prev = await previous_track()
                        await websocket.send(json.dumps(prev))
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
