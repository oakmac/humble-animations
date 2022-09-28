(ns humble-animations.main
  (:require
    [humble-animations.animate :as animate]
    [humble-animations.ui :as animate-ui]
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

(defonce *window
  (atom nil))

(defn redraw!
  "Requests a redraw on the next available frame."
  []
  (some-> @*window window/request-frame))

(animate/set-request-frame-fn! redraw!)

;; TODO: is there an empty component I can use instead of label = "" ?
(defn Box [color]
  (ui/rect (paint/fill color) (ui/label "")))

(def initial-app-state
  {:red-box-height-px 50
   :red-box-width-px 50

   :red-box-left-pct 0
   :red-box-top-pct 0})

(def *app-state
  (atom initial-app-state))

(defn reset-app-state! []
  (reset! *app-state initial-app-state)
  (redraw!))

(def layout-padding-px 10)

(def locations
  {:top-left     {:left 0   :top 0}
   :top-right    {:left 1   :top 0}
   :center       {:left 0.5 :top 0.5}
   :bottom-left  {:left 0   :top 1}
   :bottom-right {:left 1   :top 1}})

(def red-box-animate-speed-ms 250)

(defn animate-red-box-location!
  ([]
   (animate-red-box-location! 0 0))
  ([new-left-pct new-top-pct]
   (let [animation-mode (rand-nth [:single-vals :vector :map])
         {:keys [red-box-left-pct red-box-top-pct]} @*app-state]
     (case animation-mode
       :single-vals ;; Option 1) animate each property individually
       (do
         (animate/start-animation!
           {:start-val red-box-left-pct
            :end-val new-left-pct
            :duration-ms red-box-animate-speed-ms
            :on-tick (fn [{:keys [_time val]}]
                       (swap! *app-state assoc :red-box-left-pct val))
            :transition :ease-out-pow})
         (animate/start-animation!
           {:start-val red-box-top-pct
            :end-val new-top-pct
            :duration-ms red-box-animate-speed-ms
            :on-tick (fn [{:keys [_time val]}]
                       (swap! *app-state assoc :red-box-top-pct val))
            :transition :ease-out-pow
            ;; optional on-stop function will fire when the animation has finished
            :on-stop (fn [anim]
                       (println "Animation has finished:")
                       (println (pr-str anim)))}))

       :vector ;; Option 2) animate multiple properties using a Vector
       (animate/start-animation!
         {:start-val [red-box-left-pct red-box-top-pct]
          :end-val [new-left-pct new-top-pct]
          :duration-ms red-box-animate-speed-ms
          :on-tick (fn [{:keys [_time val]}]
                     (swap! *app-state assoc :red-box-left-pct (first val)
                                             :red-box-top-pct (second val)))
          :transition :ease-out-pow})

       :map ;; Option 3) animate multiple properties using a Map
       (animate/start-animation!
         {:start-val {:left red-box-left-pct :top red-box-top-pct}
          :end-val {:left new-left-pct :top new-top-pct}
          :duration-ms red-box-animate-speed-ms
          :on-tick (fn [{:keys [_time val]}]
                     (swap! *app-state assoc :red-box-left-pct (:left val)
                                             :red-box-top-pct (:top val)))
          :transition :ease-out-pow})))))

(defn animate-red-box-size!
  [width-px height-px]
  (let [{:keys [red-box-width-px red-box-height-px]} @*app-state]
    (animate/start-animation!
      {:start-val [red-box-width-px red-box-height-px]
       :end-val [width-px height-px]
       :duration-ms red-box-animate-speed-ms
       :on-tick (fn [{:keys [_time val]}]
                  (swap! *app-state assoc :red-box-width-px (first val)
                                          :red-box-height-px (second val)))
       :transition :ease-out-pow})))

(def ButtonsColumn
  (ui/padding layout-padding-px
    (ui/valign 0
      (ui/column
        (ui/button #(animate-red-box-location! 0 0) (ui/label "Top Left"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-location! 1 0) (ui/label "Top Right"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-location! 0.5 0.5) (ui/label "Center"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-location! 0 1) (ui/label "Bottom Left"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-location! 1 1) (ui/label "Bottom Right"))
        (ui/gap 0 10)
        (ui/button
          (fn []
            (let [rand-left (/ (rand-int 101) 100)
                  rand-top (/ (rand-int 101) 100)]
              (animate-red-box-location! rand-left rand-top)))
          (ui/label "Random!"))

        (ui/gap 0 30)

        (ui/button #(animate-red-box-size! 25 25) (ui/label "Small Box"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-size! 50 50) (ui/label "Medium Box"))
        (ui/gap 0 10)
        (ui/button #(animate-red-box-size! 100 100) (ui/label "Large Box"))))))

(def Separator
  (ui/rect (paint/fill light-grey)
    (ui/gap 2 0)))

(def RedBox
  (ui/dynamic _ctx [box-width (:red-box-width-px @*app-state)
                    box-height (:red-box-height-px @*app-state)]
    (ui/clip-rrect 4
      (ui/rect (paint/fill red)
        (ui/gap box-width box-height)))))

(def AnimationArea
  (ui/padding layout-padding-px
    (ui/stack
      (ui/dynamic _ctx [box-left (:red-box-left-pct @*app-state)
                        box-top (:red-box-top-pct @*app-state)]
        (ui/halign box-left
          (ui/valign box-top
            RedBox))))))

(def HumbleAnimations
  "top-level component"
  (ui/default-theme {}
    ;; animation wrapper component is required in order to call animate/tick! after every draw operation
    (animate-ui/animation-ticker
     (ui/row
       ButtonsColumn
       Separator
       [:stretch 1 AnimationArea]))))

;; re-draw the UI when we load this namespace
(redraw!)

(defn -main [& args]
  (ui/start-app!
    (reset! *window
      (ui/window
        {:title    "HumbleUI Animations"
         :bg-color 0xFFFFFFFF}
        #'HumbleAnimations))))
