package dev.karmanov.tgsimpleapiintellijplugin.reference;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import dev.karmanov.tgsimpleapiintellijplugin.inspection.util.ActionRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ActionNamePsiReference extends PsiReferenceBase<PsiLiteralExpression> {
    private final String actionName;

    public ActionNamePsiReference(@NotNull PsiLiteralExpression element, @NotNull String actionName) {
        super(element);
        this.actionName = actionName;
    }

    @Override
    public @Nullable PsiElement resolve() {
        Project project = myElement.getProject();
        Set<String> allActions = ActionRegistry.getRegisteredActions(project);

        if (!allActions.contains(actionName)) return null;

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        for (String annotationFqn : ActionRegistry.getAnnotationFqns()) {
            PsiClass annotationClass = facade.findClass(annotationFqn, scope);
            if (annotationClass == null) continue;

            for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope)) {
                PsiAnnotation annotation = method.getAnnotation(annotationFqn);
                if (annotation == null) continue;

                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("actionName");
                if (value == null) {
                    value = annotation.findAttributeValue("actionName");
                }
                if (value instanceof PsiLiteralExpression literal) {
                    Object rawValue = literal.getValue();
                    if (actionName.equals(rawValue)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        return ActionRegistry.getRegisteredActions(myElement.getProject()).toArray();
    }
}
