package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module

public data class ModuleDeclaration(
    public val path: ProjectPath,
    public val module: Module,
)
