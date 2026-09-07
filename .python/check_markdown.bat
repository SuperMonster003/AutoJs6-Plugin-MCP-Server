@ECHO OFF
CHCP 65001 >NUL
WHERE python >NUL 2>NUL
IF ERRORLEVEL 1 (
    py "%~dp0generate_markdown.py" --check
) ELSE (
    python "%~dp0generate_markdown.py" --check
)
IF ERRORLEVEL 1 (
    ECHO.
    ECHO Markdown sources and generated files are out of sync.
    ECHO Run .python\generate_markdown.bat to regenerate them.
    EXIT /B 1
)
EXIT /B 0
