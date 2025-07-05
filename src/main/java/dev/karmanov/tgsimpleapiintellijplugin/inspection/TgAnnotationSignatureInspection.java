package dev.karmanov.tgsimpleapiintellijplugin.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class TgAnnotationSignatureInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Map<String, Predicate<PsiMethod>> ANNOTATION_CHECKS = Map.of(
            "dev.karmanov.library.annotation.userActivity.BotText", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotPhoto", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotCallBack", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotDocument", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotLocation", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotMedia", TgAnnotationSignatureInspection::isSingleUpdateParam,
            "dev.karmanov.library.annotation.userActivity.BotScheduled",TgAnnotationSignatureInspection::isSetOfLong,
            "dev.karmanov.library.annotation.userActivity.BotVoice", TgAnnotationSignatureInspection::isSingleUpdateParam

    );

    private static final Map<String, String> ANNOTATION_EXPECTATIONS = Map.of(
            "dev.karmanov.library.annotation.userActivity.BotText", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotPhoto", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotCallBack", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotDocument", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotLocation", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotMedia", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotVoice", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotScheduled", "one parameter of type Set<Long>"
    );


    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                PsiModifierList modifierList = method.getModifierList();
                for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                    String qName = annotation.getQualifiedName();
                    if (qName == null) continue;

                    Predicate<PsiMethod> validator = ANNOTATION_CHECKS.get(qName);
                    if (validator != null && !validator.test(method)) {
                        String shortName = qName.substring(qName.lastIndexOf('.') + 1);
                        String expected = ANNOTATION_EXPECTATIONS.getOrDefault(qName, "specific parameters");

                        holder.registerProblem(
                                Objects.requireNonNull(method.getNameIdentifier()),
                                "The method with the annotation @" + shortName + " must have " + expected + ".",
                                ProblemHighlightType.ERROR
                        );
                    }
                }
            }
        };
    }

    private static boolean isSingleUpdateParam(PsiMethod method) {
        PsiParameterList params = method.getParameterList();
        return params.getParametersCount() == 1 &&
                params.getParameters()[0].getType().equalsToText("org.telegram.telegrambots.meta.api.objects.Update");
    }

    private static boolean isSetOfLong(PsiMethod method) {
        PsiParameterList params = method.getParameterList();
        if (params.getParametersCount() != 1) {
            return false;
        }

        PsiType paramType = params.getParameters()[0].getType();
        if (!(paramType instanceof PsiClassType classType)) {
            return false;
        }

        PsiClass resolvedClass = classType.resolve();

        if (resolvedClass == null || !CommonClassNames.JAVA_UTIL_SET.equals(resolvedClass.getQualifiedName())) {
            return false;
        }

        PsiType[] parameters = classType.getParameters();
        return parameters.length == 1 &&
                parameters[0].equalsToText(CommonClassNames.JAVA_LANG_LONG);
    }

}
