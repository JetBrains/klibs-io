INSERT INTO project_marker (project_id, type)
SELECT project_id, 'GRANT_WINNER_2023'
FROM project_index
WHERE (project_index.name = 'coil' AND owner_login = 'coil-kt')
   OR (project_index.name = 'KMP-NativeCoroutines' AND owner_login = 'rickclephas')
   OR (project_index.name = 'lyricist' AND owner_login = 'adrielcafe');

INSERT INTO project_marker (project_id, type)
SELECT project_id, 'GRANT_WINNER_2024'
FROM project_index
WHERE (project_index.name = 'Store' AND owner_login = 'MobileNativeFoundation')
   OR (project_index.name = 'multiplatform-settings' AND owner_login = 'russhwolf')
   OR (project_index.name = 'compose-rich-editor' AND owner_login = 'MohamedRejeb')
   OR (project_index.name = 'orbit-mvi' AND owner_login = 'orbit-mvi')
   OR (project_index.name = 'ultron' AND owner_login = 'open-tool');

INSERT INTO project_marker (project_id, type)
SELECT project_id, 'FEATURED'
FROM project_index
WHERE (project_index.name = 'compose-multiplatform' AND owner_login = 'JetBrains')
   OR (project_index.name = 'kotlinx.coroutines' AND owner_login = 'Kotlin')
   OR (project_index.name = 'ktor' AND owner_login = 'ktorio')
   OR (project_index.name = 'coil' AND owner_login = 'coil-kt')
   OR (project_index.name = 'koin' AND owner_login = 'InsertKoinIO')
   OR (project_index.name = 'arrow' AND owner_login = 'arrow-kt')
   OR (project_index.name = 'sqldelight' AND owner_login = 'sqldelight')
   OR (project_index.name = 'kotlinx.serialization' AND owner_login = 'Kotlin')
   OR (project_index.name = 'Store' AND owner_login = 'MobileNativeFoundation')
   OR (project_index.name = 'voyager' AND owner_login = 'adrielcafe')
   OR (project_index.name = 'turbine' AND owner_login = 'cashapp')
   OR (project_index.name = 'kotlinx-datetime' AND owner_login = 'Kotlin')
   OR (project_index.name = 'Decompose' AND owner_login = 'arkivanov')
   OR (project_index.name = 'Decompose' AND owner_login = 'arkivanov')
   OR (project_index.name = 'multiplatform-settings' AND owner_login = 'russhwolf')
   OR (project_index.name = 'Ktorfit' AND owner_login = 'Foso')
   OR (project_index.name = 'kotlin-inject' AND owner_login = 'evant')
   OR (project_index.name = 'KMP-NativeCoroutines' AND owner_login = 'rickclephas')
   OR (project_index.name = 'haze' AND owner_login = 'chrisbanes')
   OR (project_index.name = 'compose-cupertino' AND owner_login = 'alexzhirkevich')
   OR (project_index.name = 'Kermit' AND owner_login = 'touchlab')
   OR (project_index.name = 'SKIE' AND owner_login = 'touchlab')
   OR (project_index.name = 'KMP-ObservableViewModel' AND owner_login = 'rickclephas')
   OR (project_index.name = 'compose-hot-reload' AND owner_login = 'JetBrains')
   OR (project_index.name = 'kobweb' AND owner_login = 'varabyte');
