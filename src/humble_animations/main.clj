(ns humble-animations.main
  (:require
    [humble-animations.animate :as animate]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.window :as window]))

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
  ; (println "redraw!")
  (some-> @*window window/request-frame))

(animate/set-redraw-fn! redraw)

;; TODO: is there an empty component I can use instead of label = "" ?
(defn Box [color]
  (ui/rect (paint/fill color) (ui/label "")))

(def initial-app-state
  {:red-box-height 50
   :red-box-width 50

   :red-box-left-pct 0
   :red-box-top-pct 0})

(def *app-state
  (atom initial-app-state))

(defn reset-app-state! []
  (reset! *app-state initial-app-state)
  (redraw))

(def layout-padding-px 10)

(def locations
  {:top-left     {:left 0   :top 0}
   :top-right    {:left 1   :top 0}
   :center       {:left 0.5 :top 0.5}
   :bottom-left  {:left 0   :top 1}
   :bottom-right {:left 1   :top 1}})

(def red-box-animate-speed-ms 250)

(defn go-to-location!
  ([]
   (go-to-location! 0 0))
  ([new-left-pct new-top-pct]
   (let [{:keys [red-box-left-pct red-box-top-pct]} @*app-state]
     (animate/start-animation!
       {:start-val red-box-left-pct
        :end-val new-left-pct
        :duration-ms red-box-animate-speed-ms
        :on-tick (fn [{:keys [val]}]
                   (swap! *app-state assoc :red-box-left-pct val))
        :transition :ease-out-pow})
     (animate/start-animation!
       {:start-val red-box-top-pct
        :end-val new-top-pct
        :duration-ms red-box-animate-speed-ms
        :on-tick (fn [{:keys [val]}]
                   (swap! *app-state assoc :red-box-top-pct val))
        :transition :ease-out-pow}))))

(def ButtonsColumn
  (ui/padding layout-padding-px
    (ui/valign 0
      (ui/column
        (ui/button #(go-to-location! 0 0) (ui/label "Top Left"))
        (ui/gap 0 10)
        (ui/button #(go-to-location! 1 0) (ui/label "Top Right"))
        (ui/gap 0 10)
        (ui/button #(go-to-location! 0.5 0.5) (ui/label "Center"))
        (ui/gap 0 10)
        (ui/button #(go-to-location! 0 1) (ui/label "Bottom Left"))
        (ui/gap 0 10)
        (ui/button #(go-to-location! 1 1) (ui/label "Bottom Right"))))))

(def Separator
  (ui/rect (paint/fill light-grey)
    (ui/gap 2 0)))

(def RedBox
  (ui/dynamic _ctx [box-width (:red-box-width @*app-state)
                    box-height (:red-box-height @*app-state)]
    (ui/clip-rrect 8
      (ui/rect (paint/fill red)
        (ui/gap box-width box-height)))))

; (def BlueBox
;   (ui/dynamic _ctx [box-width (:red-box-width @*app-state)
;                     box-height (:red-box-height @*app-state)]
;     (ui/clip-rrect 8
;       (ui/rect (paint/fill blue)
;         (ui/gap box-width box-height)))))
;
; (def BlueBoxContainer
;   (ui/row
;     (ui/gap 100 0)
;     BlueBox))

(def AnimationArea
  (ui/padding layout-padding-px
    (ui/stack
      (ui/dynamic _ctx [box-left (:red-box-left-pct @*app-state)
                        box-top (:red-box-top-pct @*app-state)]
        (ui/halign box-left
          (ui/valign box-top
            RedBox))))))

   ; (ui/center BlueBoxContainer))))))

(def HumbleAnimations
  "top-level component"
  (ui/default-theme {}
    (ui/row
      ButtonsColumn
      Separator
      [:stretch 1 AnimationArea])))

;; re-draw the UI when we load this namespace
(redraw)

(defn -main [& args]
  (reset! *window
    (ui/start-app!
      {:title    "HumbleUI Animations"
       :bg-color 0xFFFFFFFF}
      #'HumbleAnimations)))
