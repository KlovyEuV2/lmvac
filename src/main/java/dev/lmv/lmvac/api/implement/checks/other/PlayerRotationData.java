package dev.lmv.lmvac.api.implement.checks.other;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerRotationData {
    public List<Double> deltaYaws = new CopyOnWriteArrayList<>();
    public List<Double> deltaPitches = new CopyOnWriteArrayList<>();
    public double buffer1 = 0.0;
    public double buffer2 = 0.0;
    public float lastYaw = 0.0f;
    public float lastPitch = 0.0f;

    public float deltaYaw = 0.0f;
    public float deltaPitch = 0.0f;
    public long lastSmooth = 0L;
    public long lastHighRate = 0L;
    public double lastDeltaXRot = 0.0;
    public double lastDeltaYRot = 0.0;
    public List<Double> yawSamples = new ArrayList<>();
    public List<Double> pitchSamples = new ArrayList<>();
    public boolean cinematicRotation = false;
    public int isTotallyNotCinematic = 0;
}