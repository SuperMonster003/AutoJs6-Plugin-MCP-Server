"""Long recordings for the result ceiling of `speech_transcribe` (3-Macaw Speech roadmap P7.6.7, appendix D Q41).

A transcription grows fastest when its segments are short and many: every segment costs about 55 units of JSON
besides its text, and the plugin takes at most 30 minutes of audio (its roadmap D83). The word track is therefore a
list of single words spoken fast, each followed by a pause long enough for the silero VAD to end a segment (shorter
pauses merge neighbouring words), repeated to just under 30 minutes; the over-long track is the same words for longer
than 30 minutes. On a OnePlus OPD2413 with SenseVoice the word track gave about 92 units of JSON per second, 160 Ki
units in all: the largest result real audio can give, below the 192 Ki ceiling.

Windows only: the words are spoken by the SAPI voice Microsoft Zira (System.Speech through PowerShell) and the
tracks are encoded by ffmpeg with libopus (Ogg Opus, which the plugin decodes since its P7.5), so that a 30 minute
track stays small enough for the chunked upload of `tool_calls.write_file`.
"""
from __future__ import annotations

import array
import pathlib
import subprocess
import wave

SAMPLE_RATE = 16000
WORDS = ("garden window yellow silver market people winter summer number letter orange purple dinner simple rabbit "
         "pocket circle bottle hammer jacket kitten ladder magnet napkin pencil rocket saddle tablet button candle "
         "doctor engine forest guitar island jungle kettle lemon mirror needle office parrot planet record school "
         "spider ticket valley wallet zipper anchor basket camera desert farmer harbor insect jewel kingdom meadow "
         "narrow ocean pepper quiet river sunset tunnel velvet wonder apple baker cabin daisy eagle fabric giant "
         "honey igloo joker koala lizard mango nephew olive panda quilt radio salad tiger uncle violin walrus "
         "yogurt zebra").split()
VOICE = "Microsoft Zira Desktop"
RATE = 5
GAP_MS = 350
EDGE_MS = 20
THRESHOLD = 300
WORD_SECONDS = 1790
OVER_SECONDS = 1830
BITRATE = "24k"

_SPEAK = r"""
param([string]$Dir, [string]$Words, [string]$Voice, [int]$Rate)
Add-Type -AssemblyName System.Speech
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$s.SelectVoice($Voice)
$s.Rate = $Rate
$fmt = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
$i = 0
foreach ($w in ($Words -split ' ')) {
  $s.SetOutputToWaveFile((Join-Path $Dir ("{0:D3}.wav" -f $i)), $fmt)
  $s.Speak($w)
  $i++
}
$s.SetOutputToNull()
$s.Dispose()
"""


def spoken_words(directory: pathlib.Path) -> list[array.array]:
    """The 16 kHz mono samples of every word, cut to the voice with EDGE_MS of silence on each side."""
    directory.mkdir(parents=True, exist_ok=True)
    script = directory / "speak.ps1"
    script.write_text(_SPEAK, encoding="utf-8")
    subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(script), "-Dir", str(directory),
                    "-Words", " ".join(WORDS), "-Voice", VOICE, "-Rate", str(RATE)], check=True, capture_output=True, timeout=300)
    edge = SAMPLE_RATE * EDGE_MS // 1000
    words = []
    for index in range(len(WORDS)):
        with wave.open(str(directory / f"{index:03d}.wav")) as source:
            samples = array.array("h", source.readframes(source.getnframes()))
        loud = [i for i, sample in enumerate(samples) if abs(sample) > THRESHOLD]
        if loud:
            words.append(samples[max(0, loud[0] - edge):min(len(samples), loud[-1] + edge)])
    return words


def word_track(words: list[array.array], seconds: float) -> tuple[array.array, int]:
    """The words in turn, each followed by GAP_MS of silence, up to `seconds`; returns the samples and the word count."""
    gap = array.array("h", [0]) * (SAMPLE_RATE * GAP_MS // 1000)
    limit, track, count = int(seconds * SAMPLE_RATE), array.array("h"), 0
    while len(track) + len(words[count % len(words)]) + len(gap) <= limit:
        track.extend(words[count % len(words)])
        track.extend(gap)
        count += 1
    return track, count


def encode_opus(samples: array.array, directory: pathlib.Path, name: str) -> bytes:
    """Ogg Opus bytes of the samples, through a WAV file and ffmpeg."""
    source = directory / f"{name}.wav"
    with wave.open(str(source), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(SAMPLE_RATE)
        out.writeframes(samples.tobytes())
    target = directory / f"{name}.ogg"
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(source), "-c:a", "libopus", "-b:a", BITRATE, str(target)],
                   check=True, capture_output=True, timeout=600)
    source.unlink()
    return target.read_bytes()


def long_recordings(directory: pathlib.Path) -> dict:
    """The word track of WORD_SECONDS and the over-long track of OVER_SECONDS, as Ogg Opus bytes with their facts."""
    words = spoken_words(directory / "words")
    track, count = word_track(words, WORD_SECONDS)
    over, _ = word_track(words, OVER_SECONDS)
    return {
        "words": encode_opus(track, directory, "words"),
        "wordCount": count,
        "wordSeconds": round(len(track) / SAMPLE_RATE, 1),
        "over": encode_opus(over, directory, "over"),
        "overSeconds": round(len(over) / SAMPLE_RATE, 1),
    }
