package dev.karmanov.tgsimpleapiintellijplugin.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import dev.karmanov.tgsimpleapiintellijplugin.inspection.util.ActionRegistry;
import org.jetbrains.annotations.NotNull;

public class UnregisteredActionInspectionTool extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                   boolean isOnTheFly,
                                                   @NotNull LocalInspectionToolSession session) {
        return new JavaElementVisitor() {

            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if (!"addActionData".equals(expression.getMethodExpression().getReferenceName())) return;

                PsiExpression[] arguments = expression.getArgumentList().getExpressions();
                if (arguments.length == 0) return;

                PsiExpression firstArg = arguments[0];

                if (firstArg instanceof PsiLiteralExpression literal && literal.getValue() instanceof String singleActionName) {
                    checkActionName(singleActionName, literal);
                    return;
                }

                if (firstArg instanceof PsiMethodCallExpression methodCall) {
                    String methodName = methodCall.getMethodExpression().getReferenceName();
                    PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                    String qualifierName = (qualifier instanceof PsiReferenceExpression qRef) ? qRef.getReferenceName() : "";

                    if (("of".equals(methodName) && "Set".equals(qualifierName))
                            || ("singleton".equals(methodName) && "Collections".equals(qualifierName))) {

                        for (PsiExpression expr : methodCall.getArgumentList().getExpressions()) {
                            if (expr instanceof PsiLiteralExpression lit && lit.getValue() instanceof String actionName) {
                                checkActionName(actionName, lit);
                            }
                        }
                    }
                }
            }
            private void checkActionName(String actionName, PsiElement element) {
                if (!ActionRegistry.isRegistered(actionName, element.getProject())) {
                    holder.registerProblem(element,
                            "Action name '" + actionName + "' is not registered in any known handler",
                            ProblemHighlightType.WARNING);
                }
            }
        };
    }
}
