package com.google.gcloud.datastore;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.services.datastore.DatastoreV1;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * A key that is guaranteed to be complete and could be used to reference a
 * Google Cloud Datastore {@link Entity}.
 * This class is immutable.
 *
 * @see <a href="https://cloud.google.com/datastore/docs/concepts/entities">Google Cloud Datastore Entities, Properties, and Keys</a>
 */
public final class Key extends PartialKey {

  private static final long serialVersionUID = 3160994559785491356L;

  public static final class Builder extends BaseKey.Builder<Builder> {

    private String name;
    private Long id;

    private Builder(String dataset, String kind, String name) {
      super(dataset, kind);
      this.name = name;
    }

    private Builder(String dataset, String kind, long id) {
      super(dataset, kind);
      this.id = id;
    }

    private Builder(Key copyFrom) {
      super(copyFrom);
      if (copyFrom.hasId()) {
        id = copyFrom.id();
      } else {
        name = copyFrom.name();
      }
    }

    public Builder name(String name) {
      this.name = name;
      id = null;
      return this;
    }

    public Builder id(long id) {
      this.id = id;
      name = null;
      return this;
    }

    @Override
    public Key build() {
      ImmutableList.Builder<PathElement> pathBuilder =
          ImmutableList.<PathElement>builder().addAll(ancestors);
      if (id == null) {
        pathBuilder.add(PathElement.of(kind, name));
      } else {
        pathBuilder.add(PathElement.of(kind, id));
      }
      return new Key(dataset, namespace, pathBuilder.build());
    }
  }

  Key(String dataset, String namespace, ImmutableList<PathElement> path) {
    super(dataset, namespace, path);
    Preconditions.checkArgument(nameOrId() != null);
  }

  public boolean hasId() {
    return leaf().hasId();
  }

  /**
   * Returns the key's id or {@code null} if it has a name instead.
   */
  public Long id() {
    return leaf().id();
  }

  public boolean hasName() {
    return leaf().hasName();
  }

  /**
   * Returns the key's name or {@code null} if it has an id instead.
   */
  public String name() {
    return leaf().name();
  }

  /**
   * Returns the key's id (as {@link Long}) or name (as {@link String}).
   * Never {@code null}.
   */
  public Object nameOrId() {
    return leaf().nameOrId();
  }

  /**
   * Returns the key in an encoded form that can be used as part of a URL.
   */
  public String toUrlSafe() {
    try {
      return URLEncoder.encode(toString(), UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unxpeced encoding exception", e);
    }
  }

  /**
   * Create a {@code Key} given its URL safe encoded form.
   *
   * @throws IllegalArgumentException when decoding fails
   */
  public static Key fromUrlSafe(String urlSafe) {
    try {
      String utf8Str = URLDecoder.decode(urlSafe, UTF_8.name());
      DatastoreV1.Key keyPb = DatastoreV1.Key.parseFrom(ByteString.copyFromUtf8(utf8Str));
      return fromPb(keyPb);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unxpeced decoding exception", e);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Could not parse key", e);
    }
  }

  @Override
  protected Object fromPb(byte[] bytesPb) throws InvalidProtocolBufferException {
    return fromPb(DatastoreV1.Key.parseFrom(bytesPb));
  }

  static Key fromPb(DatastoreV1.Key keyPb) {
    PartialKey key = PartialKey.fromPb(keyPb);
    Preconditions.checkState(key instanceof Key, "Key is not complete");
    return (Key) key;
  }

  public static Builder builder(String dataset, String kind, String name) {
    return new Builder(dataset, kind, name);
  }

  public static Builder builder(String dataset, String kind, long id) {
    return new Builder(dataset, kind, id);
  }

  public static Builder builder(Key copyFrom) {
    return new Builder(copyFrom);
  }

  public static Builder builder(Key parent, String kind, String name) {
    Builder builder = builder(parent.dataset(), kind, name);
    addParentToBuilder(parent, builder);
    return builder;
  }

  public static Builder builder(Key parent, String kind, long id) {
    Builder builder = builder(parent.dataset(), kind, id);
    addParentToBuilder(parent, builder);
    return builder;
  }

  private static void addParentToBuilder(Key parent, Builder builder) {
    builder.namespace(parent.namespace());
    builder.ancestors(parent.ancestors());
    if (parent.hasId()) {
      builder.ancestors(PathElement.of(parent.kind(), parent.id()));
    } else {
      builder.ancestors(PathElement.of(parent.kind(), parent.name()));
    }
  }
}
