import numpy as np
import pyaudio
from scipy.fft import fft
import wave

def record_audio(duration=5, sample_rate=44100):
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paInt16, channels=1, rate=sample_rate, input=True, frames_per_buffer=1024)
    frames = []

    for _ in range(int(sample_rate / 1024 * duration)):
        data = stream.read(1024)
        frames.append(data)

    stream.stop_stream()
    stream.close()
    p.terminate()

    audio_data = np.frombuffer(b''.join(frames), dtype=np.int16)
    return audio_data, sample_rate

def decode_audio(audio_data, sample_rate, freq0=1000, freq1=2000, duration=0.2):
    chunk_size = int(sample_rate * duration)
    binary = ''

    for i in range(0, len(audio_data), chunk_size):
        chunk = audio_data[i:i + chunk_size]
        if len(chunk) == 0:
            continue

        # 计算FFT频谱
        spectrum = np.abs(fft(chunk))[:chunk_size // 2]
        freqs = np.fft.fftfreq(len(spectrum), 1 / sample_rate)[:chunk_size // 2]

        # 找出主要频率
        dominant_freq = freqs[np.argmax(spectrum)]
        if abs(dominant_freq - freq0) < abs(dominant_freq - freq1):
            binary += '0'
        else:
            binary += '1'

    # 改进二进制转文本部分
    text = ""
    for i in range(0, len(binary), 8):
        byte = binary[i:i + 8]
        if len(byte) == 8:
            try:
                char = chr(int(byte, 2))
                if 32 <= ord(char) <= 126:  # 过滤掉非可见字符
                    text += char
            except ValueError:
                continue
    return text

# 示例：录音并解码
audio_data, sample_rate = record_audio(duration=5)
decoded_text = decode_audio(audio_data, sample_rate)
print("接收到wifi:", decoded_text)

# 将解码后的数据保存到 wifi.txt 文件中
with open("wifi.txt", "w", encoding="utf-8") as file:
    file.write(decoded_text)