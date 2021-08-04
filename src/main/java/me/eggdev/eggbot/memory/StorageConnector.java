package me.eggdev.eggbot.memory;

public abstract class StorageConnector<I, O> {

    public void write(I in) {
        O out = toOut(in);

        // todo implement database algorithm
    }

    public I read() {
        return null;
    }

    protected abstract O toOut(I in);
    protected abstract I fromOut(O out);
}
