# Pure compiler

This program compiles pure code to binary executables. Try it in the [playground](https://pure.minding.blog/playground/)!

## Command line interface

### Sub-commands

- `run <path> <entrypoint>`
- `build <path> <entrypoint>`
- `print <source|ast|llvm-ir> <path> <entrypoint>`
- `help [<sub-command>]`

### Options

- `--output-directory <path>` (default: `./out`)
- `--compile-time-debug-output`
- `--runtime-debug-output`
- `--log-level <debug|info|warning|error>` (default: `info`)

## Environment variables

- `LANG_MODULE_PATH` (optional; default: `base-library/lang`)
	- Value: `{string}`
	- Description: Path to the "Lang" module of the [base library](https://github.com/Minding000/pure-base-library).

## Dependencies (Linux)

- openjdk
- lld
- libc6
- gcc
