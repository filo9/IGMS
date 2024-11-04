import numpy as np
import sounddevice as sd
import wave
import struct

# 配置参数
SAMPLE_RATE = 48000  # 与Android端保持一致
DURATION_MS = 10  # 每比特的持续时间（毫秒）
FREQ0 = 500  # 表示“0”的频率
FREQ1 = 10000  # 表示“1”的频率
AMPLITUDE = 32767  # 音频振幅


# 将字符串转换为二进制字符串
def string_to_binary(text):
    return ''.join(format(ord(char), '08b') for char in text)


# 生成特定频率的音频信号
def generate_tone(frequency, duration_ms):
    num_samples = int(SAMPLE_RATE * (duration_ms / 1000.0))
    t = np.linspace(0, duration_ms / 1000.0, num_samples, endpoint=False)
    waveform = (AMPLITUDE * np.sin(2 * np.pi * frequency * t)).astype(np.int16)
    return waveform


# 播放并保存音频信号
def send_and_save_audio_signal(binary_data, filename="audio_signal.wav"):
    audio_data = np.array([], dtype=np.int16)

    # 根据二进制数据生成音频信号
    for bit in binary_data:
        frequency = FREQ0 if bit == '0' else FREQ1
        tone = generate_tone(frequency, DURATION_MS)
        audio_data = np.concatenate((audio_data, tone))

    # 播放音频信号
    sd.play(audio_data, samplerate=SAMPLE_RATE)
    sd.wait()

    # 将音频信号保存为 .wav 文件
    with wave.open(filename, 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)  # 每个样本 2 字节 (16 位)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(audio_data.tobytes())

    print(f"音频信号已保存为 {filename}")


# 主程序
if __name__ == "__main__":
    account = input("请输入WiFi账号: ")
    password = input("请输入WiFi密码: ")
    message = f"{account}:{password}"

    # 转换为二进制数据并发送音频信号
    binary_data = string_to_binary(message)
    send_and_save_audio_signal(binary_data)