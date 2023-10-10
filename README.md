# D Package Search

An indexer and search API for D programming language packages as registered on https://code.dlang.org, using Apache Lucene.

## Setup

To set up and run the program, all you need is Java version 21 or higher, and then run the project using your favorite IDE. It will boot up a web server that you can use to search for packages at http://localhost:8080/search?query=test, replacing `query=test` with what you want to search for.

## Architecture

The basic architecture of this searcher is that of your classic indexed search engine, which is usually comprised of the following steps:
1. Fetch raw data from somewhere.
2. Generate an index from that data.
3. Search for relevant data using the index.

In this application, steps 1 and 2 are done periodically in a separate thread, to ensure that the data stays relatively fresh. Step 3 is done whenever a request to the `/search` endpoint is received.
