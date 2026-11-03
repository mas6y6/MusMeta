package com.mas6y6.musmeta.launch;

public interface LaunchTransformer {

    byte[] transform(String name, byte[] bytes);
}