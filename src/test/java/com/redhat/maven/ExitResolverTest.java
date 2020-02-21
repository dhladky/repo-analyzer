package com.redhat.maven;

public class ExitResolverTest extends App.ExitResolver
{
    @Override
    void finishProcessing(int value) {
        throw new EndAppException(value);
    }
}
