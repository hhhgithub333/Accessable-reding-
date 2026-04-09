# import os
# import re
# import argparse
# import json
# import numpy as np
# import librosa
# import soundfile as sf
# import onnxruntime
# from transformers import AutoTokenizer
# from tqdm import trange
#
# # 直接使用本地模型路径
# LOCAL_MODEL_PATH = r"D:\Python\Project\chatterbox-turbo-ONNX"
# ONNX_DIR = os.path.join(LOCAL_MODEL_PATH, "onnx")
#
# SAMPLE_RATE = 24000
# START_SPEECH_TOKEN = 6561
# STOP_SPEECH_TOKEN = 6562
# SILENCE_TOKEN = 4299
# NUM_KV_HEADS = 16
# HEAD_DIM = 64
#
# REFERENCE_AUDIO_PATH = r"D:\Python\Project\tts_test_output\vibevoice_output.wav"
#
#
# class RepetitionPenaltyLogitsProcessor:
#     def __init__(self, penalty: float):
#         self.penalty = penalty
#
#     def __call__(self, input_ids: np.ndarray, scores: np.ndarray) -> np.ndarray:
#         score = np.take_along_axis(scores, input_ids, axis=1)
#         score = np.where(score < 0, score * self.penalty, score / self.penalty)
#         scores_processed = scores.copy()
#         np.put_along_axis(scores_processed, input_ids, score, axis=1)
#         return scores_processed
#
#
# def clean_text(text: str) -> str:
#     """清理文本，移除 GBK 无法编码的字符"""
#     # 替换不间断空格
#     text = text.replace('\xa0', ' ')
#     # 保留：ASCII、中文、常见标点
#     text = re.sub(r'[^\x00-\x7F\u4e00-\u9fff\u3000-\u303f\uff0c\uff0e\uff01\uff1f]', '', text)
#     # 合并多个空格
#     text = re.sub(r'\s+', ' ', text).strip()
#     return text
#
#
# def main():
#     parser = argparse.ArgumentParser()
#     parser.add_argument("--text", type=str, required=True)
#     parser.add_argument("--output", type=str, required=True)
#     args = parser.parse_args()
#
#     try:
#         # 清理文本
#         clean_text_content = clean_text(args.text)
#         print(f"原始文本长度: {len(args.text)}, 清理后长度: {len(clean_text_content)}")
#
#         if not clean_text_content:
#             raise ValueError("清理后文本为空")
#
#         # 检查本地模型文件
#         if not os.path.exists(ONNX_DIR):
#             raise FileNotFoundError(f"ONNX 目录不存在: {ONNX_DIR}")
#
#         # 使用本地 ONNX 文件
#         conditional_decoder_path = os.path.join(ONNX_DIR, "conditional_decoder.onnx")
#         speech_encoder_path = os.path.join(ONNX_DIR, "speech_encoder.onnx")
#         embed_tokens_path = os.path.join(ONNX_DIR, "embed_tokens.onnx")
#         language_model_path = os.path.join(ONNX_DIR, "language_model.onnx")
#
#         for path in [conditional_decoder_path, speech_encoder_path, embed_tokens_path, language_model_path]:
#             if not os.path.exists(path):
#                 raise FileNotFoundError(f"模型文件不存在: {path}")
#
#         print("加载本地 ONNX 模型中...")
#
#         speech_encoder_session = onnxruntime.InferenceSession(speech_encoder_path)
#         embed_tokens_session = onnxruntime.InferenceSession(embed_tokens_path)
#         language_model_session = onnxruntime.InferenceSession(language_model_path)
#         cond_decoder_session = onnxruntime.InferenceSession(conditional_decoder_path)
#
#         print("模型加载成功")
#
#         if not os.path.exists(REFERENCE_AUDIO_PATH):
#             raise FileNotFoundError(f"参考音频不存在: {REFERENCE_AUDIO_PATH}")
#
#         # 准备参考音频
#         audio_values, _ = librosa.load(REFERENCE_AUDIO_PATH, sr=SAMPLE_RATE)
#         audio_values = audio_values[np.newaxis, :].astype(np.float32)
#
#         # 准备文本（使用本地 tokenizer）
#         tokenizer = AutoTokenizer.from_pretrained(LOCAL_MODEL_PATH, local_files_only=True)
#         input_ids = tokenizer(clean_text_content, return_tensors="np")["input_ids"].astype(np.int64)
#
#         max_new_tokens = 1024
#         repetition_penalty = 1.2
#         repetition_penalty_processor = RepetitionPenaltyLogitsProcessor(penalty=repetition_penalty)
#         generate_tokens = np.array([[START_SPEECH_TOKEN]], dtype=np.int64)
#
#         print(f"生成音频中...")
#
#         for i in trange(max_new_tokens, desc="Sampling", dynamic_ncols=True):
#             inputs_embeds = embed_tokens_session.run(None, {"input_ids": input_ids})[0]
#
#             if i == 0:
#                 ort_speech_encoder_input = {"audio_values": audio_values}
#                 cond_emb, prompt_token, speaker_embeddings, speaker_features = speech_encoder_session.run(
#                     None, ort_speech_encoder_input
#                 )
#                 inputs_embeds = np.concatenate((cond_emb, inputs_embeds), axis=1)
#
#                 batch_size, seq_len, _ = inputs_embeds.shape
#                 past_key_values = {
#                     i.name: np.zeros([batch_size, NUM_KV_HEADS, 0, HEAD_DIM],
#                                      dtype=np.float16 if i.type == 'tensor(float16)' else np.float32)
#                     for i in language_model_session.get_inputs()
#                     if "past_key_values" in i.name
#                 }
#                 attention_mask = np.ones((batch_size, seq_len), dtype=np.int64)
#                 position_ids = np.arange(seq_len, dtype=np.int64).reshape(1, -1).repeat(batch_size, axis=0)
#
#             logits, *present_key_values = language_model_session.run(None, {
#                 "inputs_embeds": inputs_embeds,
#                 "attention_mask": attention_mask,
#                 "position_ids": position_ids,
#                 **past_key_values,
#             })
#
#             logits = logits[:, -1, :]
#             next_token_logits = repetition_penalty_processor(generate_tokens, logits)
#
#             input_ids = np.argmax(next_token_logits, axis=-1, keepdims=True).astype(np.int64)
#             generate_tokens = np.concatenate((generate_tokens, input_ids), axis=-1)
#
#             if (input_ids.flatten() == STOP_SPEECH_TOKEN).all():
#                 break
#
#             attention_mask = np.concatenate([attention_mask, np.ones((batch_size, 1), dtype=np.int64)], axis=1)
#             position_ids = position_ids[:, -1:] + 1
#             for j, key in enumerate(past_key_values):
#                 past_key_values[key] = present_key_values[j]
#
#         speech_tokens = generate_tokens[:, 1:-1]
#         silence_tokens = np.full((speech_tokens.shape[0], 3), SILENCE_TOKEN, dtype=np.int64)
#         speech_tokens = np.concatenate([prompt_token, speech_tokens, silence_tokens], axis=1)
#
#         wav = cond_decoder_session.run(None, {
#             "speech_tokens": speech_tokens,
#             "speaker_embeddings": speaker_embeddings,
#             "speaker_features": speaker_features,
#         })[0].squeeze(axis=0)
#
#         sf.write(args.output, wav, SAMPLE_RATE)
#         print(f"音频已保存: {args.output}")
#
#         result = {"success": True, "output": args.output}
#         print(json.dumps(result))
#
#     except Exception as e:
#         result = {"success": False, "error": str(e)}
#         print(json.dumps(result))
#
#
# if __name__ == "__main__":
#     main()

# import os
# import argparse
# import json
# import torch
# import torchaudio as ta
# from chatterbox.tts_turbo import ChatterboxTurboTTS
#
# REFERENCE_AUDIO_PATH = r"D:\Python\Project\tts_test_output\vibevoice.wav"
# def main():
#     parser = argparse.ArgumentParser()
#     parser.add_argument("--text", type=str, required=True)
#     parser.add_argument("--output", type=str, required=True)
#     args = parser.parse_args()
#
#     try:
#         # 官方方式：只传 device
#         device = "cuda" if torch.cuda.is_available() else "cpu"
#         print(f"使用设备: {device}")
#
#         print("加载 ChatterBox Turbo 模型中...")
#         model = ChatterboxTurboTTS.from_pretrained(device=device)
#         print("模型加载成功")
#
#         # 生成音频（不需要参考音频也可以）
#         wav = model.generate(
#             args.text,
#             audio_prompt_path = REFERENCE_AUDIO_PATH
#         )
#
#         ta.save(args.output, wav, model.sr)
#         print(f"音频已保存: {args.output}")
#
#         result = {"success": True, "output": args.output}
#         print(json.dumps(result))
#
#     except Exception as e:
#         result = {"success": False, "error": str(e)}
#         print(json.dumps(result))
#
#
# if __name__ == "__main__":
#     main()

import os
import argparse
import json
import torch
import torchaudio as ta
from chatterbox.tts_turbo import ChatterboxTurboTTS

# REFERENCE_AUDIO_PATH = r"D:\Python\Project\tts_test_output\vibevoice.wav"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", type=str, required=True)
    parser.add_argument("--output", type=str, required=True)
    args = parser.parse_args()

    try:
        device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"使用设备: {device}")

        print("加载 ChatterBox Turbo 模型中...")
        model = ChatterboxTurboTTS.from_pretrained(device=device)
        print("模型加载成功")

        # 限制文本长度，避免长文本导致噪音
        text = args.text
        max_chars = 300  # 限制 300 字符
        if len(text) > max_chars:
            text = text[:max_chars]
            print(f"文本过长，已截断至 {max_chars} 字符")

        # 添加生成参数，尝试稳定输出
        wav = model.generate(
            text,
            # audio_prompt_path=REFERENCE_AUDIO_PATH,
            cfg_weight=0.5,      # 降低 CFG 权重，提高稳定性
            exaggeration=0.5,    # 降低夸张度
            temperature=0.7      # 降低温度
        )

        ta.save(args.output, wav, model.sr)
        print(f"音频已保存: {args.output}")

        result = {"success": True, "output": args.output}
        print(json.dumps(result))

    except Exception as e:
        result = {"success": False, "error": str(e)}
        print(json.dumps(result))

if __name__ == "__main__":
    main()