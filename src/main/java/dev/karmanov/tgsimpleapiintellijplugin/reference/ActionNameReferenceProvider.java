package dev.karmanov.tgsimpleapiintellijplugin.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class ActionNameReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                           @NotNull ProcessingContext context) {
        if (!(element instanceof PsiLiteralExpression literal)) return PsiReference.EMPTY_ARRAY;

        Object value = literal.getValue();
        if (!(value instanceof String actionName)) return PsiReference.EMPTY_ARRAY;

        return new PsiReference[]{ new ActionNamePsiReference(literal, actionName) };
    }
}
