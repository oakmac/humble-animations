(ns humble-animations.main
  (:require
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.window :as window]))

    ; [tween-clj.core :as tween]))

(set! *warn-on-reflection* true)

(reset! debug/*enabled? true)

(def dark-grey 0xff404040)
(def light-grey 0xffeeeeee)
(def blue 0xff0d7fbe)
(def yellow 0xfffae317)
(def red 0xfff50f0f)
(def white 0xfff3f3f3)
(def black 0xff000000)

(def outside-padding 15)
(def inner-padding 10)

(defonce *window
  (atom nil))

(defn redraw []
  (println "redraw!")
  (some-> @*window window/request-frame))

;; TODO: is there an empty component I can use instead of label = "" ?
(defn Box [color]
  (ui/rect (paint/fill color) (ui/label "")))



(def default-rectangle-opts
  {:bg-color red
   :height-px 150
   :width-px 300})

(defn Rectangle2
  ([]
   (Rectangle2 default-rectangle-opts))
  ([{:keys [bg-color height-px width-px] :as _opts}]
   (ui/rect
    (paint/fill bg-color)
    (ui/gap 0 0))))

(def start
  {:x 0})

(def end
  {:x 100})

(def *app-state
  (atom {:x 300}))

(def HumbleAnimations
  "top-level component"
  (ui/default-theme {}
    (clickable/clickable
      {:on-click
       (fn [evt]
         (println "Mouse event:")
         (swap! *app-state update-in [:x] inc)
         (println (pr-str @*app-state))
         (redraw))}
         ; (println (pr-str evt)))}
      (ui/rect (paint/fill light-grey)
        (ui/padding outside-padding
          ; (Box blue)
          (ui/dynamic _ctx [x (:x @*app-state)]
            (ui/row
              (ui/gap x 0)
              [:stretch 1 (Box blue)])))))))

        ; (ui/row
        ;   [:stretch 1 (Box blue)]
        ;   (ui/gap inner-padding 0)
        ;   [:stretch 1 (Box yellow)]
        ;   (ui/gap inner-padding 0)
        ;   [:stretch 1 (Rectangle2 {:bg-color dark-grey})])))))

;; re-draw the UI when we load this namespace
(redraw)

(defn -main [& args]
  (reset! *window
    (ui/start-app!
      {:title    "HumbleUI Animations"
       :bg-color 0xFFFFFFFF}
      #'HumbleAnimations)))
