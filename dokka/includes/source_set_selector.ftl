<#macro display>
    <div class="links-section" id="links-section">
        <div class="wiki-section-wrapper" id="wiki-section-wrapper" title="Open Fzzy Config wiki">
            <a class="wiki-section" href="https://github.com/fzzyhmstrs/fconfig/wiki" rel="nofollow">
            </a>
        </div>
        <div class="modrinth-section-wrapper" id="modrinth-section-wrapper" title="Open Modrinth page">
            <a class="modrinth-section" href="https://modrinth.com" rel="nofollow">
            </a>
        </div>
        <div class="cf-section-wrapper" id="cf-section-wrapper" title="Open Curseforge page">
            <a class="cf-section" href="https://curseforge.com" rel="nofollow">
            </a>
        </div>
        <div class="discord-section-wrapper" id="discord-section-wrapper" title="Discord invite link">
            <a class="discord-section" href="https://discord.gg/Fnw2UbE72x" rel="nofollow">
            </a>
        </div>
        <#if sourceSets?has_content>
            <div class="filter-section" id="filter-section">
                <#list sourceSets as ss>
                    <button class="platform-tag platform-selector ${ss.platform}-like" data-active="" data-filter="${ss.filter}">${ss.name}</button>
                </#list>
            </div>
        </#if>
    </div>

</#macro>