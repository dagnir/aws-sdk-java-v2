# Project Tenets (unless you know better ones)

1. Meeting customers in their problem space allows them to deliver value
   quickly.
2. Meeting customer expectations drives usability.
3. Discoverability drives usage.

# Introduction

This project provides a much improved experience for S3 customers needing to
easily perform uploads and downloads of objects to and from S3 by providing the
S3 `S3TransferManager`, a high level library built on the S3 client.

# Project Goals

1. For the use cases it addresses, i.e. the transfer of objects to and from S3,
   S3TransferManager is the preferred solution. It is easier and more intuitive
   than using the S3 client. In the majority of situations, it is more
   performant.
1. S3TransferManager provides a truly asynchronous, non-blocking API that
   conforms to the norms present in the rest of the SDK.
1. S3TransferManager makes efficient use of system resources.
1. S3TransferManager supplements rather than replaces the lower level S3 client.

# Non Project Goals

1. Ability to use the blocking, synchronous client.

   Using a blocking client would severely impede the ability to deliver on goals
   #2 and #3.

# Customer-Requested Changes from 1.11.x

* S3TransferManager supports progress listeners that are easier to use.

  Ref: https://github.com/aws/aws-sdk-java-v2/issues/37#issuecomment-316218667

* S3TransferManager provides bandwidth limiting of uploads and downloads.

  Ref: https://github.com/aws/aws-sdk-java/issues/1103

* The size of resources used by Transfermanager and configured by the user
  should not affect its stability.

  For example, the configured size of a threadpool should be irellevant to its
  ability to successfuly perform an operation.

  Ref: https://github.com/aws/aws-sdk-java/issues/939

* S3TransferManager supports parallel downloads of any object.

  Any object stored in S3 should be downloadable in multiple parts
  simultaneously, not just those uploaded using the Multipart API.

* S3TransferManager has the ability to upload to and download from a pre-signed
  URL.

* S3TransferManager allows uploads and downloads from and to memory.

  Ref: https://github.com/aws/aws-sdk-java/issues/474

* Ability to easily use canned ACL policies with all transfers to S3.

  Ref: https://github.com/aws/aws-sdk-java/issues/1207

* Trailing checksums for parallel uploads and downloads.

# Design Walkthrough

This section is intended as a walkthrough of the current `S3TransferManager` design prototype in order to go into more detail about the key elements in the interfaces.

If you're familiar with `TransferManager` from v1 of the Java SDK, then a lot of this may look familiar.

To see the prototype of the interfaces, click [here](prototype.java).

## Basic Operations

The two basic, key operations in `S3TransferManager` are `download` and `upload`, which are used to upload and download objects to and from S3:

```java
Download download(String bucket, String key, Path file);

Upload upload(String bucket, String key, Path file);
```

As one might expect, these two methods allow users to easily upload and download files to and from S3, possibly the two most common operations for users of `TransferManager` and S3 in general.

The interface is intended to be as simple as possible, especially for the most common use cases; without needing to worry about `getObject`, `AsyncRequestBody`, or `AsyncResponseTransformer`, the interface streamlines the process for downloading and uploading a file to a few simple parameters. Under the hood, `S3TransferManager` takes care of the details, such as whether to download or upload in multiple parts, proper object partitioning in the case of a multipart transfer, limiting the bandwidth taken up by the transfer, ensuring to limit the concurrency of requests to avoid timeouts, and so on.

As the `S3TransferManager` evolves, more overloads will be added to support a larger variety of "sources" and "destinations". For example, a common feature request for v1's `TransferManager` is the ability to [upload to S3 directly from an `InputStream` without first buffering on disk, and without knowing the content length][v1_474]. Such a method could look like

```java
Upload upload(String bucket, String key, InputStream is);
```

Due to the different limitations of some source and destination types however, it may be difficult or impossible to provide the same features for all of them. For example, the `upload` implementation for `InputStream` will be impossible to do parallel multipart uploads for it without buffering data into memory.

Although the "simplified" methods will probably be sufficient for most use cases, there are times when customers will need more control and options. For more control and options when performing `download` and `upload`, the `S3TransferManager` will expose request objects that will look familiar to anyone who has seen the usual request objects of the low level clients. These request objects are intended to encapsulate all the movable knobs of the respective operations that they represent:

```java
Download download(DownloadRequest request, Path file);

Upload upload(UploadRequest request, Path file);
```

Here is one area where this version of `TransferManager` differs from the current implementation. In v1, the `TransferManager` takes S3's request objects. For example, in v1, `download` takes `GetObjectRequest`. This is a little awkward because they're at different abstraction levels. In v2, we introduce request objects that are specific to TransferManager so we can include transfer manager specific things to the request. These request object in turn will take the low level requests as one of their properties.

`DownloadRequest` for example, has an optional `size` property that represents the size of the object to be downloaded from S3. Normally, (this was true for v1), the transfer manager must query S3 using a `HeadObjectRequest` for this information as it's a key piece of data when multipart downloads are enabled. However, performing an extra HTTP request incurs overhead and impacts the performance of the download, so if customers know the size of the given object upfront, then they can provide it via the `DownloadRequest` so `S3TransferManager` can skip the `HEAD` request.

Just like the low level client requests, the transfer manager's request object also allow customers to override some transfer manager configurations at the request level. The example below sets a different bandwidth limit for the download:

```java
TransferOverrideConfiguration overrideConfig = TransferOverrideConfiguration.builder()
                                                                             .maxTransferBytesPerSecond(4 * SizeConstant.MiB)
                                                                             .build();
DownloadRequest request = DownloadRequest.builder()
                                         .overrideConfiguration(overrideConfig)
                                         ... // set other properties
                                         .build()

Download dl = transferManager.download(request, myFile);
```

Another facet of the interfaces above that will look familiar to v1's `TransferManager` are the returns types of `download` and `upload`, which are `Download` and `Upload` respectively, the same names used in v1. Likewise, both objects `extend` the same class, `Transfer`, from which all operations' return types will extend, e.g. `downloadDirectory`.

The superclass `Transfer` has the following interface:

```java
public interface Transfer {
    /**
     * @return The future that will be completed when this transfer is complete.
     */
    CompletableFuture<? extends CompletedTransfer> completionFuture();
}
```

As with anything described in this document, the interface may evolve over the course of the implementation phase of `TransferManager`, but at the moment, a base `Transfer` contains a single method, `completionFuture()`. As the name return type suggests, this is the `CompletableFuture` that is completed once the transfer is complete.

The use of `CompletableFuture` makes the following things natural and intuitive:

 - Transfers can be easily canceled by canceling the future

   This is behavior carried over from the async clients, where any running operation can be canceled by calling the `cancel()` method on the returned future.

 - Waiting for the transfer to complete synchronously is acomplished easily using `get()` or `join()`

 - Dependent operations can be chained easily

   Standard `CompletableFuture` operations like `wheComplete`, `thenHandle`, etc are all there.

For comparison v1's `Transfer` class has two separate `wait*` methods: `waitForException` and `waitForCompletion`, both of which essentially do the same thing, but behave a little differently, which at the very least is confusing.

The future itself is completed with an instance of `CompletedTransfer`:

```java
public interface CompletedTransfer {
    /**
     * The metrics for this transfer.
     */
    TransferMetrics metrics();
}
```

A `CompletedTransfer` is intended to contain data related to the transfer that can only be accessed or supplied after it is complete; here we see that all completed transfers expose some metrics about them, such as total time to complete.

## Performance

Something that is not immediately apparent from only looking at the interface is the focus we have on ensuring that `TransferManager` performs as well as possible. We want to ensure that not only is `TransferManager` easy to use, it also helps customers make the most out of their hardware by being efficient, and performant.

[v1_474]: https://github.com/aws/aws-sdk-java/issues/474
