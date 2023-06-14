(ns io.github.rutledgepaulv.transiting.core
  (:require [cognitect.transit :as transit])
  (:import (clojure.lang TaggedLiteral)
           (com.cognitect.transit DefaultReadHandler ReadHandler WriteHandler)
           (com.github.luben.zstd ZstdInputStream ZstdOutputStream)
           (java.time Instant)
           (java.util.regex Pattern)))

(def PatternHandler
  (reify
    WriteHandler
    (tag [this value]
      "pattern")
    (rep [this value]
      [(str value) (.flags ^Pattern value)])
    ReadHandler
    (fromRep [this [value flags]]
      (Pattern/compile value flags))))

(def InstantHandler
  (reify
    WriteHandler
    (tag [this value]
      "instant")
    (rep [this value]
      (str value))
    ReadHandler
    (fromRep [this value]
      (Instant/parse value))))

(def TaggedLiteralHandler
  (reify WriteHandler
    (tag [this value]
      (str (.-tag ^TaggedLiteral value)))
    (rep [this value]
      (.-form ^TaggedLiteral value))
    ReadHandler
    (fromRep [this value]
      value)))

(def DefaultHandler
  (reify
    WriteHandler
    (tag [this value]
      (throw (ex-info "No handler found for type" {:type (type value)})))
    (rep [this value]
      (throw (ex-info "No handler found for type" {:type (type value)})))
    DefaultReadHandler
    (fromRep [this tag rep]
      (tagged-literal (symbol tag) rep))))

(def WriteHandlers
  {Pattern       PatternHandler
   Instant       InstantHandler
   TaggedLiteral TaggedLiteralHandler
   ; work around edge case in transit-java
   ; where object's supertype is null and
   ; so it encodes it as null rather than
   ; throwing an error
   Object        DefaultHandler})

(def ReadHandlers
  {(.tag PatternHandler "") PatternHandler
   (.tag InstantHandler "") InstantHandler})

(defn encode [data output-stream]
  (with-open [compressed (ZstdOutputStream. output-stream)]
    (let [options {:handlers        WriteHandlers
                   :default-handler DefaultHandler}
          writer  (transit/writer compressed :msgpack options)]
      (transit/write writer data))))

(defn decode [input-stream]
  (with-open [compressed (ZstdInputStream. input-stream)]
    (let [options {:handlers        ReadHandlers
                   :default-handler DefaultHandler}
          reader  (transit/reader compressed :msgpack options)]
      (transit/read reader))))