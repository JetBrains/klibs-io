package io.klibs.integration.mcp.tool

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.integration.mcp.dto.api.ProjectSearchResponse
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpProjectSearchService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class McpProjectSearchTool(
    private val mcpProjectSearchService: McpProjectSearchService,
    private val mcpToolMapper: McpToolMapper
) {

    @Tool(
        description = """Searches for Kotlin Multiplatform projects by keywords, platforms/targets, and kotlin version.
            Request arguments additional information:
              Available groups and their targets:
              - JVM: 1.6, 1.7, 1.8, 9-24
              - AndroidJvm: 1.6, 1.7, 1.8, 9-24
              - IOS: ios_arm32, ios_arm64, ios_x64, ios_simulator_arm64
              - MacOS: macos_arm64, macos_x64
              - Linux: linux_arm32_hfp, linux_arm64, linux_mips32, linux_mipsel32, linux_x64
              - Windows: mingw_x64, mingw_x86
              - JavaScript: js_ir, js_legacy, js_pre_ir
              - TvOS: tvos_arm64, tvos_simulator_arm64, tvos_x64
              - WatchOS: watchos_arm32, watchos_arm64, watchos_device_arm64, watchos_simulator_arm64, watchos_x64, watchos_x86
              - AndroidNative: android_arm32, android_arm64, android_x64, android_x86
              - Wasm: wasm32
              Example: {"JVM": ["11", "17"], "IOS": ["ios_arm64"]}
              To filter by group only (any target within): {"IOS": [], "JVM": []}
            
            Response fields per project:
            - projectName: Name of the project
            - projectAuthor: Owner/author login
            - kotlinVersion: Kotlin version used by the project (from latest package), or null if unavailable
            - platforms: List of supported platforms
            - targets: List of supported targets
            - packages: Up to 200 packages (groupId, artifactId, description) belonging to the project
            - readme: Project README content in Markdown format, or null if unavailable"""
    )
    fun searchProjects(
        @ToolParam(description = "Free text search keywords (e.g. 'serialization', 'state machine', 'compose ui').")
        query: String? = null,
        @ToolParam(
            description = "List of platform filters. Allowed values: common, jvm, androidJvm, native, wasm, js.",
            required = false
        )
        platforms: List<String>? = emptyList(),
        @ToolParam(
            description = "Map of target group to specific targets. " +
                    "Keys are target groups (e.g. 'JVM', 'IOS'), " +
                    "values are sets of specific targets within that group. " +
                    "Empty set means any target in the group. Example: {\"JVM\": [\"11\", \"17\"], \"IOS\": []}.",
            required = false
        )
        targetFilters: Map<TargetGroup, Set<String>>? = emptyMap(),
    ): ProjectSearchResponse {
        val parsedPlatforms =
            platforms?.map { PackagePlatform.findBySerializableName(it) }.orEmpty()
        val result =
            mcpProjectSearchService.mcpProjectSearch(query, parsedPlatforms, targetFilters.orEmpty())
        return mcpToolMapper.mapToProjectSearchResponse(result)
    }
}
