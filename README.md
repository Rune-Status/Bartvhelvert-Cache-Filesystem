# Cache
An open source library for editing the OSRS cache filesystem

### How to use
First, load xteas into memory then open the cache with:
```kotlin
val store = CacheStore(FileStore.open(CACHE_ROOT_DIR), XTEAS)
```

To read an archive from the cache: 
```kotlin
val refTable = store.getReferenceTable(IDX_FILE_ID)
val entry = refTable.getEntry(INDEX_IN_ARCHIVE)
val archive = Archive.decode(
  store.read(IDX_FILE_ID, INDEX_IN_ARCHIVE).data, 
  refTable.getEntry(INDEX_IN_ARCHIVE)!!.amountOfChildren
)
```

### Important
- Calling the Library from Java is currently not supported
- The library only works with the current OSRS cache, RS2 hybrid caches are not supported
- The name of all your cache files should start with `main_file_cache`, if this is not the case you are probably trying to load a cache which is not supported.

### Maven
```xml
<dependency>
  <groupId>io.github.runedata</groupId>
  <artifactId>cache-filesystem</artifactId>
  <version>1.0.1</version>
</dependency>
```

### Gradle
```groovy
implementation group: 'io.github.runedata', name: 'cache-filesystem', version: '1.0.1'
```
