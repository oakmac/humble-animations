(ns humble-animations.ui
  "Creates the animation-ticker wrapper component, which is used to trigger animate/tick!
  after a draw operation"
  (:require
    [humble-animations.animate :as animate]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types IRect]
    [java.lang AutoCloseable]))

(core/deftype+ AnimationTicker [child ^:mut ^IRect child-rect]
  protocols/IContext
  (-context [_ ctx]
    ctx)

  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))

  (-draw [this ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas)
    ;; call animate/tick! after we have drawn this components children
    (animate/tick!))

  (-event [_ ctx event]
    (core/event-child child ctx event))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))

  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn animation-ticker [child]
  (->AnimationTicker child nil))
