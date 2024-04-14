<#macro display>
    <div class="links-section" id="links-section">
        <div class="discord-section-wrapper" id="discord-section-wrapper">
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