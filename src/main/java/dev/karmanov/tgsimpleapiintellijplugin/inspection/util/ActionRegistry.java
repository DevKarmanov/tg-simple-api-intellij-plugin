package dev.karmanov.tgsimpleapiintellijplugin.inspection.util;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistry {

    private static final Set<String> TG_ANNOTATIONS = Set.of(
            "dev.karmanov.library.annotation.userActivity.BotCallBack",
            "dev.karmanov.library.annotation.userActivity.BotDocument",
            "dev.karmanov.library.annotation.userActivity.BotLocation",
            "dev.karmanov.library.annotation.userActivity.BotMedia",
            "dev.karmanov.library.annotation.userActivity.BotPhoto",
            "dev.karmanov.library.annotation.userActivity.BotText",
            "dev.karmanov.library.annotation.userActivity.BotVoice"
    );

    public static Set<String> getAnnotationFqns() {
        return TG_ANNOTATIONS;
    }

    private static final Map<Project, CachedActionSet> cache = new ConcurrentHashMap<>();

    private static final long CACHE_LIFETIME_MS = 10000;

    public static boolean isRegistered(String actionName, Project project) {
        return getRegisteredActions(project).contains(actionName);
    }

    public static Set<String> getRegisteredActions(Project project) {
        CachedActionSet cached = cache.get(project);

        long now = System.currentTimeMillis();

        if (cached != null && now - cached.timestamp < CACHE_LIFETIME_MS) {
            return cached.actions;
        }

        Set<String> freshActions = DumbService.getInstance(project)
                .runReadActionInSmartMode(() -> collectAllRegisteredActions(project));

        cache.put(project, new CachedActionSet(freshActions, now));
        return freshActions;
    }

    private static Set<String> collectAllRegisteredActions(Project project) {
        Set<String> registeredActions = new HashSet<>();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

        for (String annotationFqn : TG_ANNOTATIONS) {
            PsiClass annotationClass = psiFacade.findClass(annotationFqn, scope);
            if (annotationClass == null) continue;

            Query<PsiMethod> methodsWithAnnotation = AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope);

            for (PsiMethod method : methodsWithAnnotation) {
                PsiAnnotation annotation = method.getAnnotation(annotationFqn);
                if (annotation == null) continue;

                PsiAnnotationMemberValue value = annotation.findAttributeValue("actionName");
                if (value instanceof PsiLiteralExpression literal) {
                    Object rawValue = literal.getValue();
                    if (rawValue instanceof String actionName) {
                        registeredActions.add(actionName);
                    }
                }
            }
        }

        return registeredActions;
    }

    private record CachedActionSet(Set<String> actions, long timestamp) {
    }
}
