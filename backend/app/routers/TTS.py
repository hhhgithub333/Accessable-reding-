# from fastapi import APIRouter, HTTPException
# from fastapi.responses import StreamingResponse
# from ..models.schemas import TTSRequest
# from ..tts import get_engine, get_engines_info
#
# router = APIRouter(prefix="/tts", tags=["TTS"])
#
#
# @router.get("/engines")
# async def get_engines():
#     return get_engines_info()
#
#
# @router.post("/synthesize")
# async def synthesize(request: TTSRequest):
#     try:
#         print(f"收到请求: engine={request.engine}, voice={request.voice}, text={request.text[:50]}...")
#
#         engine = get_engine(request.engine)
#         audio_data = await engine.synthesize(request.text, request.voice)
#
#         print(f"合成成功，音频大小: {len(audio_data)} bytes")
#
#         return StreamingResponse(
#             iter([audio_data]),
#             media_type="audio/mp3",
#             headers={"X-Engine": request.engine, "X-Voice": request.voice}
#         )
#
#     except ValueError as e:
#         raise HTTPException(status_code=400, detail=str(e))
#     except Exception as e:
#         print(f"TTS 合成失败: {e}")
#         raise HTTPException(status_code=500, detail=str(e))


from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from ..models.schemas import TTSRequest
from ..tts import get_engine, get_engines_info

router = APIRouter(prefix="/tts", tags=["TTS"])


@router.get("/engines")
async def get_engines():
    return get_engines_info()


@router.post("/synthesize")
async def synthesize(request: TTSRequest):
    try:
        print(f"收到请求: engine={request.engine}, voice={request.voice}, text={request.text[:50]}...")

        engine = get_engine(request.engine)
        audio_data = await engine.synthesize(request.text, request.voice)

        print(f"合成成功，音频大小: {len(audio_data)} bytes")

        audio_format = engine.get_audio_format()
        media_type = "audio/mpeg" if audio_format == "mp3" else "audio/wav"

        return StreamingResponse(
            iter([audio_data]),
            media_type=media_type,
            headers={"X-Engine": request.engine, "X-Voice": request.voice, "X-Format": audio_format}
        )

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        import traceback
        error_full = traceback.format_exc()
        print(f"TTS 合成失败: {error_full}")
        # 返回完整错误信息
        raise HTTPException(status_code=500, detail=str(e) + "\n" + error_full)