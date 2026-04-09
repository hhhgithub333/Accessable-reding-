# import requests
#
# # 注册
# resp = requests.post(
#     "http://localhost:8000/user/register",
#     json={"username": "test", "password": "123456"}
# )
# print("注册:", resp.json())
#
# # 登录
# resp = requests.post(
#     "http://localhost:8000/user/login",
#     json={"username": "test", "password": "123456"}
# )
# print("登录:", resp.json())

import os
import argparse
import json
import torch
import torchaudio as ta
from chatterbox.tts_turbo import ChatterboxTurboTTS

def main():
    # 短文本测试
    text = "The Clockwork Heart In the heart of a city that never slept, where neon lights flickered like dying stars and the rain always fell in a gentle, persistent whisper, there was a small, dusty shop. Tucked between a noodle bar and a tailor, it was easy to miss."
    output = "test_short2.wav"

    try:
        device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"使用设备: {device}")

        print("加载 ChatterBox Turbo 模型中...")
        model = ChatterboxTurboTTS.from_pretrained(device=device)
        print("模型加载成功")

        # 不使用参考音频
        wav = model.generate(text)

        ta.save(output, wav, model.sr)
        print(f"音频已保存: {output}")

    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    main()