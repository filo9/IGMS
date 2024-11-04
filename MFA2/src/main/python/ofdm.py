import numpy as np
import sounddevice as sd
import wave

# OFDM 参数
SAMPLE_RATE = 44100       # 采样率（Hz）
NUM_SUBCARRIERS = 3       # 子载波数量
SYMBOL_DURATION = 0.3     # 每个符号的持续时间（秒）
AMPLITUDE = 0.2           # 信号振幅

# 生成子载波频率
subcarrier_frequencies = np.linspace(1000, 5000, NUM_SUBCARRIERS)

# 将字符串转换为二进制数据
def string_to_binary(text):
    return ''.join(format(ord(char), '08b') for char in text)

# OFDM 调制函数
def ofdm_modulate(binary_data):
    # 将二进制数据分为与子载波数量相等的块
    data_chunks = [binary_data[i:i + NUM_SUBCARRIERS] for i in range(0, len(binary_data), NUM_SUBCARRIERS)]
    ofdm_signal = np.array([])

    for chunk in data_chunks:
        # 如果最后一块数据不够子载波数，补齐
        if len(chunk) < NUM_SUBCARRIERS:
            chunk += '0' * (NUM_SUBCARRIERS - len(chunk))

        # 将数据映射到子载波上
        symbol = np.array([1 if bit == '1' else -1 for bit in chunk])
        t = np.arange(int(SYMBOL_DURATION * SAMPLE_RATE)) / SAMPLE_RATE
        
        # 生成每个子载波的调制信号，并叠加得到 OFDM 符号
        ofdm_symbol = np.sum([AMPLITUDE * s * np.sin(2 * np.pi * f * t) for s, f in zip(symbol, subcarrier_frequencies)], axis=0)
        
        # 添加到 OFDM 信号中
        ofdm_signal = np.concatenate((ofdm_signal, ofdm_symbol))

    return ofdm_signal

# 播放 OFDM 信号
def play_ofdm_signal(ofdm_signal):
    sd.play(ofdm_signal, samplerate=SAMPLE_RATE)
    sd.wait()

# 保存 OFDM 信号到 WAV 文件
def save_ofdm_signal(ofdm_signal, filename="ofdm_signal.wav"):
    # 将信号缩放到 16 位整型并转换格式
    scaled_signal = (ofdm_signal * np.iinfo(np.int16).max).astype(np.int16)
    with wave.open(filename, 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)  # 每个样本 2 字节 (16 位)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(scaled_signal.tobytes())
    print(f"音频信号已保存为 {filename}")

# 主程序
if __name__ == "__main__":
    # 输入要发送的信息
    account = input("请输入WiFi账号: ")
    password = input("请输入WiFi密码: ")
    message = f"{account}:{password}"

    # 转换为二进制数据并生成 OFDM 信号
    binary_data = string_to_binary(message)
    ofdm_signal = ofdm_modulate(binary_data)

    # 播放 OFDM 信号
    print("正在发送 OFDM 音频信号...")
    play_ofdm_signal(ofdm_signal)
    print("发送完成")

    # 保存 OFDM 信号到文件
    save_ofdm_signal(ofdm_signal)