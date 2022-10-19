# Radixal
A Radix-notation based MIDI editor<br/>

## Installation
There's no pip version yet so you'll need to use the included buildscript
```bash
./buildscript.py --local
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

