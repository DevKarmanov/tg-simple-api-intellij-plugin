package dev.karmanov.tgsimpleapiintellijplugin.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

public class TgAnnotationSignatureInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Map<String, BiPredicate<PsiMethod, PsiAnnotation>> ANNOTATION_CHECKS = Map.of(
            "dev.karmanov.library.annotation.userActivity.BotText", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotPhoto", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotCallBack", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotDocument", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotLocation", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotMedia", (m, a) -> isSingleUpdateParam(m),
            "dev.karmanov.library.annotation.userActivity.BotScheduled", (m, a) -> isSetOfLong(m),
            "dev.karmanov.library.annotation.userActivity.BotVoice", TgAnnotationSignatureInspection::isVoiceWithOptionalText
    );

    private static final Map<String, String> ANNOTATION_EXPECTATIONS = Map.of(
            "dev.karmanov.library.annotation.userActivity.BotText", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotPhoto", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotCallBack", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotDocument", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotLocation", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotMedia", "one parameter of type Update",
            "dev.karmanov.library.annotation.userActivity.BotScheduled", "one parameter of type Set<Long>",
            "dev.karmanov.library.annotation.userActivity.BotVoice",
            "either one parameter of type Update or two parameters: Update and String (if textInterpreter = true)"
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

                    BiPredicate<PsiMethod, PsiAnnotation> validator = ANNOTATION_CHECKS.get(qName);
                    if (validator != null && !validator.test(method, annotation)) {
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

    private static boolean isVoiceWithOptionalText(PsiMethod method, PsiAnnotation annotation) {
        PsiParameterList params = method.getParameterList();
        int paramCount = params.getParametersCount();

        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("textInterpreter");
        boolean textInterpreterEnabled = false;

        if (value != null) {
            String text = value.getText();
            textInterpreterEnabled = "true".equalsIgnoreCase(text) || Boolean.TRUE.toString().equalsIgnoreCase(text.replace("\"", ""));
        }

        if (textInterpreterEnabled) {
            // Должно быть два параметра: Update и String
            if (paramCount != 2) return false;

            PsiParameter[] parameters = params.getParameters();
            return parameters[0].getType().equalsToText("org.telegram.telegrambots.meta.api.objects.Update") &&
                    parameters[1].getType().equalsToText(CommonClassNames.JAVA_LANG_STRING);
        } else {
            // Только один параметр: Update
            return paramCount == 1 &&
                    params.getParameters()[0].getType().equalsToText("org.telegram.telegrambots.meta.api.objects.Update");
        }
    }
}

