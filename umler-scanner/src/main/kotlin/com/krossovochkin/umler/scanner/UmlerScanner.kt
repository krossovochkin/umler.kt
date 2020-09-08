package com.krossovochkin.umler.scanner

import com.krossovochkin.umler.core.*
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromCallableReflectionType
import org.jetbrains.kotlin.com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.translate.utils.PsiUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBinding
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.org.objectweb.asm.TypeReference
import java.io.File

class UmlerScanner : FileProcessListener {

    private val elements = mutableSetOf<PsiWrapper<Element>>()

    override fun onProcess(file: KtFile, bindingContext: BindingContext) {
        super.onProcess(file, bindingContext)

        file.accept(object : DetektVisitor() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                if (klass.isPublic) {
                    val element = when {
                        klass.isInterface() -> {
                            InterfaceElement(name = klass.name.toString())
                        }
                        else -> {
                            ClassElement(name = klass.name.toString())
                        }
                    }
                    elements.add(PsiWrapper(klass, element))
                }
            }
        })
    }

    override fun onFinish(files: List<KtFile>, result: Detektion, bindingContext: BindingContext) {
        super.onFinish(files, result, bindingContext)

        val connections = getConnections(elements, bindingContext)

        val file = File(System.getProperty("user.dir"), "umler.json")
        if (!file.exists() && !file.createNewFile()) {
            throw IllegalStateException("Can't create output file")
        }

        writeUml(
            elements.map { it.data }.toSet(),
            connections,
            file
        )
    }

    private fun getConnections(
        elements: Set<PsiWrapper<Element>>,
        bindingContext: BindingContext
    ): Set<Connection> {
        val elementFqNameMap = elements.map { it.klass.fqName.toString() to it.data }.toMap()

        val inheritanceConnections = elements
            .mapConnections(
                extract = { child ->
                    child.klass.superTypeListEntries
                },
                map = { child, parent ->
                    if (child !is InterfaceElement && parent is InterfaceElement) {
                        ImplementsConnection(
                            start = child,
                            end = parent
                        )
                    } else {
                        ExtendsConnection(
                            start = child,
                            end = parent
                        )
                    }
                },
                elementFqNameMap = elementFqNameMap,
                bindingContext = bindingContext
            )

        val aggregateConnections = elements
            .mapConnections(
                extract = { parent ->
                    mutableListOf<KtCallableDeclaration>()
                        .apply {
                            addAll(parent.klass.body?.properties.orEmpty())
                            addAll(parent.klass.primaryConstructorParameters
                                .filter { it.isPropertyParameter() })
                        }
                },
                map = { parent, child -> AggregatesConnection(child, parent) },
                elementFqNameMap = elementFqNameMap,
                bindingContext = bindingContext
            )

        val usesConnections = elements
            .mapConnections(
                extract = { parent ->
                    parent.klass.declarations.filterIsInstance<KtFunction>()
                        .flatMap { it.valueParameters }
                },
                map = { parent, child -> UsesConnection(parent, child) },
                elementFqNameMap = elementFqNameMap,
                bindingContext = bindingContext
            )

        return emptySet<Connection>() +
                inheritanceConnections +
                aggregateConnections +
                usesConnections
    }
}

private inline fun Set<PsiWrapper<Element>>.mapConnections(
    extract: (parent: PsiWrapper<Element>) -> List<KtElement>,
    map: (parent: Element, child: Element) -> Connection,
    elementFqNameMap: Map<String, Element>,
    bindingContext: BindingContext
): List<Connection> {
    return this
        .flatMap { parent ->
            extract(parent)
                .map {
                    when (it) {
                        is KtCallableDeclaration -> it.typeReference
                        is KtSuperTypeListEntry -> it.typeReference
                        else -> error("Unknown type: $it")
                    }
                }
                .map { it.resolveType(bindingContext) }
                .mapNotNull { elementFqNameMap[it] }
                .map { child -> map(parent.data, child) }
        }
}

private data class PsiWrapper<T>(
    val klass: KtClass,
    val data: T
)


private fun KtTypeReference?.resolveType(context: BindingContext): String {
    val type = this?.createTypeBinding(context)?.type
    val args = type?.arguments

    return when (args?.size) {
        0 -> type.getJetTypeFqName(false)
        1 -> args.firstOrNull()?.type?.getJetTypeFqName(false)
        else -> null
    }.toString()
}

