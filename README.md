# Radixulous
A Radix-notation based MIDI editor<br/>
[![PyPI - Downloads](https://img.shields.io/pypi/dm/radixulous?style=flat-square)](https://pypi.org/project/radixulous/)
[![PyPI](https://img.shields.io/pypi/v/radixulous?style=flat-square)](https://pypi.org/project/radixulous/)
[![PyPI - License](https://img.shields.io/pypi/l/radixulous?style=flat-square)](https://burnsomni.net/git/radixulous/?branch=master&path=LICENSE)


### Notes
Keep in mind this project is still in early days.
Some planned features are:
1) Loading a midi file into the editor
2) Linked beats/sections
3) Export
    1) specified sections
    2) with dynamics
    3) to notation editor compatible files (eg, .mscz)
4) Playback & general audio
5) Variable Radix (use base N instead of exclusively 12)


## Installation
```bash
pip install radixulous
```

To start an empty file:

```bash
radixulous
```

To Load an existing project

```bash
radixulous path/to/project/
```

## Controls
### Default Mode
- `hjlk`: Left, Up, Down, Right
- `0123456789ab`: Set active note (absolute) value
- `+-v^`: Set active note (relative) value
- `/`: Divide note/rest
- `i`: Insert rest
- `x`: Remove note
- `.`: Unset note / Set 'Rest'
- `:`: open command line
- `u`: undo


## Commands
- `swap channel_a channel_b`: Swap channels
- `c+ channel`: Add line to channel
- `c- channel`: remove line from channel
- `w [path]`: save
- `q`: quit
- `export [path] [--tempo N] [--transpose N]`: Export to midi file

