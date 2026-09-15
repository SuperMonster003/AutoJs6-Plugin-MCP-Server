<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="{{ repo_url }}/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
    </picture>
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ license_url }}"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

******

### {{ h3_status }}

******

{{ p_status }}

******

### {{ h3_features }}

******

{{ p_features_intro }}:

{{ placeholder_features }}

******

### {{ h3_tools }}

******

{{ p_tools_intro }}:

{{ placeholder_tools_table }}

******

### {{ h3_usage }}

******

{{ placeholder_usage_steps }}

> {{ p_usage_note }}

<p align="center">
  <img src="{{ repo_url }}/blob/master/docs/images/readme/drawer-{{ screenshot_language }}.png?raw=true" alt="{{ text_screenshot_drawer }}" width="300" />
  <img src="{{ repo_url }}/blob/master/docs/images/readme/settings-{{ screenshot_language }}.png?raw=true" alt="{{ text_screenshot_settings }}" width="300" />
</p>

******

### {{ h3_quick_start }}

******

{{ p_quick_start_intro }}:

```shell
adb forward tcp:{{ mcp_default_port }} tcp:{{ mcp_default_port }}
claude mcp add --transport http autojs6 http://127.0.0.1:{{ mcp_default_port }}{{ mcp_endpoint_path }} --header "Authorization: Bearer <token>"
```

{{ p_quick_start_note }}

******

### {{ h3_connection_paths }}

******

{{ p_connection_usb }}

{{ p_connection_lan }}

{{ p_connection_note }}

******

### {{ h3_clients }}

******

{{ p_clients_intro }}.

{{ p_client_claude_code }}.

{{ p_client_cursor }}:

```json
{
  "mcpServers": {
    "autojs6": {
      "url": "http://127.0.0.1:{{ mcp_default_port }}{{ mcp_endpoint_path }}",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

{{ p_client_codex }}:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:{{ mcp_default_port }}{{ mcp_endpoint_path }}"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

{{ p_client_inspector }}:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:{{ mcp_default_port }}{{ mcp_endpoint_path }} --transport http --header "Authorization: Bearer <token>" --method tools/list
```

{{ p_client_generic }}.

{{ p_client_claude_desktop }}.

******

### {{ h3_faq }}

******

{{ placeholder_faq_points }}

******

### {{ h3_security }}

******

{{ p_security_intro }}

{{ placeholder_security_points }}

{{ p_security_permission }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ plugin_application_id }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
service action: {{ plugin_service_action }}
service category: {{ plugin_service_category }}
info action: {{ plugin_info_action }}
aidl interface: {{ plugin_aidl_interface }}
minimum host build: {{ required_host_version_code }} ({{ required_host_version_name }})
default endpoint: http://127.0.0.1:{{ mcp_default_port }}{{ mcp_endpoint_path }}
```

{{ p_contract_service }}

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ roadmap_url }})

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

{{ p_build_intro }}

{{ p_build_debug }}:

```powershell
.\gradlew.bat :app:assembleDebug
```

{{ p_build_test }}:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

{{ p_build_release }}:

```powershell
.\gradlew.bat :app:assembleRelease
```

{{ p_build_digest }}:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

{{ p_build_docs_check }}:

```powershell
py .python\generate_markdown.py --check
```

{{ p_build_requirements }}

******

### {{ h3_resource_layout }}

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

{{ p_resource_layout }}

******

### {{ h3_license }}

******

{{ p_license }}

******

### {{ h3_links }}

******

- {{ text_link_autojs6 }}: {{ autojs6_url }}
- {{ text_link_autojs6_docs }}: {{ docs_autojs6_url }}
- {{ text_link_mcp_spec }}: {{ mcp_spec_url }}
- {{ text_link_third_party_notices }}: {{ third_party_notices_url }}


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
