public class CustomVertex {
    private String id;
    private String query;

    public CustomVertex(String id, String query) {
        this.id = id;
        this.query = query;
    }

    public String toString() {
        return id;
    }

    public String getQuery() {
        return query;
    }
}
